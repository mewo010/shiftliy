import 'dart:async';
import 'package:flutter/foundation.dart';
import '../models/shift.dart';
import '../models/expense.dart';
import '../services/hive_storage_service.dart';
import '../services/notification_service.dart';

class ShiftProvider extends ChangeNotifier {
  List<Shift> _shifts = [];
  Shift? _activeShift;
  bool _isBreakActive = false;
  DateTime? _breakStartTime;
  int _accumulatedBreakSeconds = 0;

  Timer? _ticker;
  Duration _elapsed = Duration.zero;
  Duration _breakElapsed = Duration.zero;

  double _hourlyRate = 45.0;
  Shift? _reviewingShift;

  final List<String> availableJobRoles = [
    'מלצרות',
    'בר',
    'מטבח',
    'אבטחה',
    'שירות לקוחות',
    'משלוחים',
    'כללי'
  ];

  ShiftProvider() {
    _loadData();
    _initNotificationHandler();
  }

  void _initNotificationHandler() {
    NotificationService().init(onActionSelected: (actionId) {
      if (actionId == 'break_action') {
        pauseForBreak();
      } else if (actionId == 'resume_action') {
        resumeFromBreak();
      } else if (actionId == 'stop_action') {
        stopShiftToReview();
      }
    });
  }

  List<Shift> get shifts => _shifts;
  Shift? get activeShift => _activeShift;
  bool get isRunning => _activeShift != null;
  bool get isBreakActive => _isBreakActive;
  Duration get elapsed => _elapsed;
  Duration get breakElapsed => _breakElapsed;
  double get hourlyRate => _hourlyRate;
  Shift? get reviewingShift => _reviewingShift;

  /// Monthly aggregated calculation helpers
  List<Shift> getShiftsForMonth(DateTime month) {
    return _shifts.where((s) {
      return s.startTime.year == month.year && s.startTime.month == month.month;
    }).toList();
  }

  double monthlyNetEarnings(DateTime month) {
    final list = getShiftsForMonth(month);
    return list.fold(0.0, (sum, s) => sum + s.netPay);
  }

  double monthlyTotalHours(DateTime month) {
    final list = getShiftsForMonth(month);
    return list.fold(0.0, (sum, s) => sum + s.netDurationHours);
  }

  double monthlyTotalTips(DateTime month) {
    final list = getShiftsForMonth(month);
    return list.fold(0.0, (sum, s) => sum + s.tips);
  }

  double monthlyTotalExpenses(DateTime month) {
    final list = getShiftsForMonth(month);
    return list.fold(0.0, (sum, s) => sum + s.totalExpenses);
  }

  double get currentMonthNetEarnings => monthlyNetEarnings(DateTime.now());
  double get currentMonthTotalHours => monthlyTotalHours(DateTime.now());
  double get currentMonthTotalTips => monthlyTotalTips(DateTime.now());
  double get currentMonthTotalExpenses => monthlyTotalExpenses(DateTime.now());

  void _loadData() {
    _hourlyRate = HiveStorageService.getHourlyRate();
    _shifts = HiveStorageService.getAllShifts();
    notifyListeners();
  }

  void setHourlyRate(double rate) {
    _hourlyRate = rate;
    HiveStorageService.setHourlyRate(rate);
    notifyListeners();
  }

  void startShift(String role, bool isBreakPaid) {
    _activeShift = Shift(
      startTime: DateTime.now(),
      jobRole: role,
      isBreakPaid: isBreakPaid,
      hourlyRate: _hourlyRate,
    );
    _isBreakActive = false;
    _breakStartTime = null;
    _accumulatedBreakSeconds = 0;
    _elapsed = Duration.zero;
    _breakElapsed = Duration.zero;

    _startTicker();
    _updateNotification();
    notifyListeners();
  }

  void _startTicker() {
    _ticker?.cancel();
    _ticker = Timer.periodic(const Duration(seconds: 1), (t) {
      if (_activeShift != null) {
        _elapsed = DateTime.now().difference(_activeShift!.startTime);
        if (_isBreakActive && _breakStartTime != null) {
          final currentBreakDuration = DateTime.now().difference(_breakStartTime!).inSeconds;
          _breakElapsed = Duration(seconds: _accumulatedBreakSeconds + currentBreakDuration);
        } else {
          _breakElapsed = Duration(seconds: _accumulatedBreakSeconds);
        }

        // Update notification every 5 seconds or immediately on state changes
        if (t.tick % 5 == 0) {
          _updateNotification();
        }

        notifyListeners();
      }
    });
  }

  void _updateNotification() {
    if (_activeShift == null) return;
    final hours = _elapsed.inHours.toString().padLeft(2, '0');
    final minutes = (_elapsed.inMinutes % 60).toString().padLeft(2, '0');
    final seconds = (_elapsed.inSeconds % 60).toString().padLeft(2, '0');
    final timeStr = '$hours:$minutes:$seconds';

    final title = _isBreakActive ? 'Shiftly - בהפסקה ($timeStr)' : 'Shiftly - משמרת פעילה ($timeStr)';
    final body = '${_activeShift!.jobRole} | תעריף: ₪${_hourlyRate.toStringAsFixed(0)}/שעה';

    NotificationService().showShiftNotification(
      title: title,
      body: body,
      isBreak: _isBreakActive,
    );
  }

  void pauseForBreak() {
    if (!isRunning || _isBreakActive) return;
    _isBreakActive = true;
    _breakStartTime = DateTime.now();
    _updateNotification();
    notifyListeners();
  }

  void resumeFromBreak() {
    if (!isRunning || !_isBreakActive) return;
    if (_breakStartTime != null) {
      _accumulatedBreakSeconds += DateTime.now().difference(_breakStartTime!).inSeconds;
      _breakStartTime = null;
    }
    _isBreakActive = false;
    _updateNotification();
    notifyListeners();
  }

  void toggleBreakType(bool isPaid) {
    if (_activeShift != null) {
      _activeShift!.isBreakPaid = isPaid;
      notifyListeners();
    }
  }

  void stopShiftToReview() {
    if (!isRunning) return;
    if (_isBreakActive && _breakStartTime != null) {
      _accumulatedBreakSeconds += DateTime.now().difference(_breakStartTime!).inSeconds;
      _breakStartTime = null;
      _isBreakActive = false;
    }

    _ticker?.cancel();
    _ticker = null;
    NotificationService().cancelShiftNotification();

    final breakMinutes = (_accumulatedBreakSeconds / 60).round();
    _reviewingShift = Shift(
      id: _activeShift!.id,
      startTime: _activeShift!.startTime,
      endTime: DateTime.now(),
      breakDurationMinutes: breakMinutes,
      isBreakPaid: _activeShift!.isBreakPaid,
      jobRole: _activeShift!.jobRole,
      hourlyRate: _hourlyRate,
      expenses: _activeShift!.expenses,
      tips: _activeShift!.tips,
      notes: _activeShift!.notes,
    );

    _activeShift = null;
    _elapsed = Duration.zero;
    _breakElapsed = Duration.zero;
    _accumulatedBreakSeconds = 0;
    notifyListeners();
  }

  void discardActiveShift() {
    _ticker?.cancel();
    _ticker = null;
    NotificationService().cancelShiftNotification();
    _activeShift = null;
    _isBreakActive = false;
    _breakStartTime = null;
    _accumulatedBreakSeconds = 0;
    _elapsed = Duration.zero;
    _breakElapsed = Duration.zero;
    notifyListeners();
  }

  void openShiftForReview(Shift shift) {
    _reviewingShift = shift;
    notifyListeners();
  }

  void dismissReviewSheet() {
    _reviewingShift = null;
    notifyListeners();
  }

  Future<void> saveShift(Shift shift) async {
    await HiveStorageService.saveShift(shift);
    _reviewingShift = null;
    _shifts = HiveStorageService.getAllShifts();
    notifyListeners();
  }

  Future<void> addParsedShifts(List<Shift> newShifts) async {
    await HiveStorageService.saveMultipleShifts(newShifts);
    _shifts = HiveStorageService.getAllShifts();
    notifyListeners();
  }

  Future<void> deleteShift(String id) async {
    await HiveStorageService.deleteShift(id);
    _shifts = HiveStorageService.getAllShifts();
    notifyListeners();
  }

  @override
  void dispose() {
    _ticker?.cancel();
    NotificationService().cancelShiftNotification();
    super.dispose();
  }
}
