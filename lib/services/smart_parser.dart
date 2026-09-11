import '../models/shift.dart';

class SmartParser {
  static const int defaultYear = 2026;

  static final RegExp _lineRegex = RegExp(
    r'(?<day>\d{1,2})[./](?<month>\d{1,2})(?:[./](?<year>\d{2,4}))?'
    r'\s*[-–—:\s]\s*'
    r'(?<startH>\d{1,2}):(?<startM>\d{2})'
    r'\s*[-–—:\s]\s*'
    r'(?<endH>\d{1,2}):(?<endM>\d{2})'
    r'(?<remainder>.*)',
  );

  static final RegExp _breakRegex = RegExp(
    r'(\d+)\s*(?:דקות|דק[\'״]|min|minutes)?',
    caseSensitive: false,
  );

  static final RegExp _noBreakRegex = RegExp(
    r'(?:ללא(?:\s+הפסקה)?|none|\b0\b|בלי)',
    caseSensitive: false,
  );

  static final RegExp _tipRegex = RegExp(
    r'(?:\+|\bטיפ(?:ים)?\b)\s*([0-9]+(?:\.[0-9]+)?)',
    caseSensitive: false,
  );

  /// Parse a single line of freeform shift log.
  static Shift? parseLine(String rawLine, {double defaultHourlyRate = 45.0}) {
    final trimmed = rawLine.trim();
    if (trimmed.isEmpty) return null;

    final match = _lineRegex.firstMatch(trimmed);
    if (match == null) return null;

    final day = int.tryParse(match.namedGroup('day') ?? '');
    final month = int.tryParse(match.namedGroup('month') ?? '');
    var year = int.tryParse(match.namedGroup('year') ?? '') ?? defaultYear;
    if (year < 100) {
      year += 2000;
    }

    final startH = int.tryParse(match.namedGroup('startH') ?? '');
    final startM = int.tryParse(match.namedGroup('startM') ?? '');
    final endH = int.tryParse(match.namedGroup('endH') ?? '');
    final endM = int.tryParse(match.namedGroup('endM') ?? '');

    if (day == null || month == null || startH == null || startM == null || endH == null || endM == null) {
      return null;
    }

    final startDateTime = DateTime(year, month, day, startH, startM);
    var endDateTime = DateTime(year, month, day, endH, endM);

    // Cross-midnight / night shifts: e.g., 22:00 - 06:00 wraps to the next calendar day
    if (endDateTime.isBefore(startDateTime) || endDateTime.isAtSameMomentAs(startDateTime)) {
      endDateTime = endDateTime.add(const Duration(days: 1));
    }

    final remainder = (match.namedGroup('remainder') ?? '').trim();
    int breakMinutes = 0;
    double tips = 0.0;

    if (remainder.isNotEmpty) {
      final tipMatch = _tipRegex.firstMatch(remainder);
      if (tipMatch != null) {
        tips = double.tryParse(tipMatch.group(1) ?? '0') ?? 0.0;
      }

      if (_noBreakRegex.hasMatch(remainder)) {
        breakMinutes = 0;
      } else {
        final breakMatch = _breakRegex.firstMatch(remainder);
        if (breakMatch != null) {
          breakMinutes = int.tryParse(breakMatch.group(1) ?? '0') ?? 0;
        }
      }
    }

    return Shift(
      startTime: startDateTime,
      endTime: endDateTime,
      breakDurationMinutes: breakMinutes,
      isBreakPaid: false,
      tips: tips,
      hourlyRate: defaultHourlyRate,
      jobRole: 'משמרת',
      notes: 'יובא אוטומטית מ-WhatsApp',
    );
  }

  /// Parse multiple lines (e.g. from WhatsApp messages).
  static List<Shift> parseMultiple(String text, {double defaultHourlyRate = 45.0}) {
    final results = <Shift>[];
    final lines = text.split(RegExp(r'[\r\n;]+'));
    for (final line in lines) {
      final shift = parseLine(line, defaultHourlyRate: defaultHourlyRate);
      if (shift != null) {
        results.add(shift);
      }
    }
    return results;
  }
}
