import 'package:hive_flutter/hive_flutter.dart';
import '../models/shift.dart';
import '../models/expense.dart';

class HiveStorageService {
  static const String shiftsBoxName = 'shifts_box';
  static const String settingsBoxName = 'settings_box';

  static Future<void> init() async {
    await Hive.initFlutter();

    if (!Hive.isAdapterRegistered(0)) {
      Hive.registerAdapter(ShiftAdapter());
    }
    if (!Hive.isAdapterRegistered(1)) {
      Hive.registerAdapter(ExpenseAdapter());
    }

    await Hive.openBox<Shift>(shiftsBoxName);
    await Hive.openBox(settingsBoxName);
  }

  static Box<Shift> get shiftsBox => Hive.box<Shift>(shiftsBoxName);
  static Box get settingsBox => Hive.box(settingsBoxName);

  static List<Shift> getAllShifts() {
    return shiftsBox.values.toList()
      ..sort((a, b) => b.startTime.compareTo(a.startTime));
  }

  static Future<void> saveShift(Shift shift) async {
    await shiftsBox.put(shift.id, shift);
  }

  static Future<void> saveMultipleShifts(List<Shift> shifts) async {
    final map = {for (var s in shifts) s.id: s};
    await shiftsBox.putAll(map);
  }

  static Future<void> deleteShift(String id) async {
    await shiftsBox.delete(id);
  }

  static double getHourlyRate() {
    return (settingsBox.get('hourly_rate', defaultValue: 45.0) as num).toDouble();
  }

  static Future<void> setHourlyRate(double rate) async {
    await settingsBox.put('hourly_rate', rate);
  }
}
