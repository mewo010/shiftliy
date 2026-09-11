// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'shift.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

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

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ShiftAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
