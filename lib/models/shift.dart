import 'package:hive/hive.dart';
import 'package:uuid/uuid.dart';
import 'expense.dart';

@HiveType(typeId: 0)
class Shift extends HiveObject {
  @HiveField(0)
  final String id;

  @HiveField(1)
  final DateTime startTime;

  @HiveField(2)
  DateTime? endTime;

  @HiveField(3)
  int breakDurationMinutes;

  @HiveField(4)
  bool isBreakPaid;

  @HiveField(5)
  List<Expense> expenses;

  @HiveField(6)
  double tips;

  @HiveField(7)
  String jobRole;

  @HiveField(8)
  String notes;

  @HiveField(9)
  double hourlyRate;

  Shift({
    String? id,
    required this.startTime,
    this.endTime,
    this.breakDurationMinutes = 0,
    this.isBreakPaid = false,
    List<Expense>? expenses,
    this.tips = 0.0,
    this.jobRole = 'משמרת',
    this.notes = '',
    this.hourlyRate = 45.0,
  })  : id = id ?? const Uuid().v4(),
        expenses = expenses ?? [];

  /// Exact elapsed time calculated to the second.
  Duration get duration {
    final end = endTime ?? DateTime.now();
    return end.difference(startTime);
  }

  /// Returns 0 if isBreakPaid is true, else breakDurationMinutes.
  int get unpaidBreakDuration {
    return isBreakPaid ? 0 : breakDurationMinutes;
  }

  /// Total duration minus unpaid breaks in hours.
  double get netDurationHours {
    final totalSec = duration.inSeconds;
    final unpaidSec = unpaidBreakDuration * 60;
    final netSec = (totalSec - unpaidSec).clamp(0, double.maxFinite.toInt());
    return netSec / 3600.0;
  }

  /// Gross pay based on hourly rate.
  double get grossPay {
    return netDurationHours * hourlyRate;
  }

  /// Total logged expenses.
  double get totalExpenses {
    return expenses.fold(0.0, (sum, exp) => sum + exp.amount);
  }

  /// Net earnings = gross earnings + tips - expenses.
  double get netPay {
    return grossPay + tips - totalExpenses;
  }
}

// Hive TypeAdapter implementation
class ShiftAdapter extends TypeAdapter<Shift> {
  @override
  final int typeId = 0;

  @override
  Shift read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return Shift(
      id: fields[0] as String?,
      startTime: fields[1] as DateTime,
      endTime: fields[2] as DateTime?,
      breakDurationMinutes: fields[3] as int? ?? 0,
      isBreakPaid: fields[4] as bool? ?? false,
      expenses: (fields[5] as List?)?.cast<Expense>() ?? [],
      tips: (fields[6] as num?)?.toDouble() ?? 0.0,
      jobRole: fields[7] as String? ?? 'משמרת',
      notes: fields[8] as String? ?? '',
      hourlyRate: (fields[9] as num?)?.toDouble() ?? 45.0,
    );
  }

  @override
  void write(BinaryWriter writer, Shift obj) {
    writer
      ..writeByte(10)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.startTime)
      ..writeByte(2)
      ..write(obj.endTime)
      ..writeByte(3)
      ..write(obj.breakDurationMinutes)
      ..writeByte(4)
      ..write(obj.isBreakPaid)
      ..writeByte(5)
      ..write(obj.expenses)
      ..writeByte(6)
      ..write(obj.tips)
      ..writeByte(7)
      ..write(obj.jobRole)
      ..writeByte(8)
      ..write(obj.notes)
      ..writeByte(9)
      ..write(obj.hourlyRate);
  }
}
