import '../models/shift.dart';

class SmartParser {
  static const int defaultYear = 2026;

  static final RegExp _lineRegex = RegExp(
    r'(\d{1,2})[./](\d{1,2})(?:[./](\d{2,4}))?'
    r'\s*[-–—:\s]\s*'
    r'(\d{1,2}):(\d{2})'
    r'\s*[-–—:\s]\s*'
    r'(\d{1,2}):(\d{2})'
    r'(.*)',
    caseSensitive: false,
  );

  static final RegExp _breakRegex = RegExp(
    '(\\d+)\\s*(?:דקות|דק|\'|"|׳|״|min|minutes)?',
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

  static Shift? parseLine(String rawLine, {double defaultHourlyRate = 45.0}) {
    final trimmed = rawLine.trim();
    if (trimmed.isEmpty) return null;

    final match = _lineRegex.firstMatch(trimmed);
    if (match == null) return null;

    final day = int.tryParse(match.group(1) ?? '');
    final month = int.tryParse(match.group(2) ?? '');
    var year = int.tryParse(match.group(3) ?? '') ?? defaultYear;
    if (year < 100) {
      year += 2000;
    }

    final startH = int.tryParse(match.group(4) ?? '');
    final startM = int.tryParse(match.group(5) ?? '');
    final endH = int.tryParse(match.group(6) ?? '');
    final endM = int.tryParse(match.group(7) ?? '');

    if (day == null || month == null || startH == null || startM == null || endH == null || endM == null) {
      return null;
    }

    final startDateTime = DateTime(year, month, day, startH, startM);
    var endDateTime = DateTime(year, month, day, endH, endM);

    if (endDateTime.isBefore(startDateTime) || endDateTime.isAtSameMomentAs(startDateTime)) {
      endDateTime = endDateTime.add(const Duration(days: 1));
    }

    final remainder = (match.group(8) ?? '').trim();
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
