import 'package:flutter/material.dart';
import 'package:intl/intl.dart' hide TextDirection;
import 'package:table_calendar/table_calendar.dart';
import '../models/shift.dart';

class CalendarScreen extends StatefulWidget {
  final List<Shift> shifts;
  final Function(Shift) onShiftClick;
  final Function(String) onDeleteShift;
  final Function(DateTime) onAddManualShift;

  const CalendarScreen({
    super.key,
    required this.shifts,
    required this.onShiftClick,
    required this.onDeleteShift,
    required this.onAddManualShift,
  });

  @override
  State<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends State<CalendarScreen> {
  DateTime _focusedDay = DateTime.now();
  DateTime? _selectedDay = DateTime.now();

  List<Shift> _getShiftsForDay(DateTime day) {
    return widget.shifts.where((s) => isSameDay(s.startTime, day)).toList();
  }

  @override
  Widget build(BuildContext context) {
    final selectedDayShifts = _selectedDay != null ? _getShiftsForDay(_selectedDay!) : <Shift>[];
    final timeFormat = DateFormat('HH:mm');

    return Column(
      children: [
        TableCalendar<Shift>(
          locale: 'he_IL',
          firstDay: DateTime.utc(2020, 1, 1),
          lastDay: DateTime.utc(2030, 12, 31),
          focusedDay: _focusedDay,
          selectedDayPredicate: (day) => isSameDay(_selectedDay, day),
          eventLoader: _getShiftsForDay,
          calendarFormat: CalendarFormat.month,
          startingDayOfWeek: StartingDayOfWeek.sunday,
          calendarStyle: CalendarStyle(
            // Highlight Saturdays with distinct purple styling for Shabbat
            weekendTextStyle: const TextStyle(color: Color(0xFF6B21A8), fontWeight: FontWeight.bold),
            todayDecoration: BoxDecoration(
              color: Theme.of(context).colorScheme.primaryContainer,
              shape: BoxShape.circle,
            ),
            selectedDecoration: BoxDecoration(
              color: Theme.of(context).colorScheme.primary,
              shape: BoxShape.circle,
            ),
            markerDecoration: const BoxDecoration(
              color: Color(0xFF10B981),
              shape: BoxShape.circle,
            ),
          ),
          headerStyle: const HeaderStyle(
            formatButtonVisible: false,
            titleCentered: true,
          ),
          onDaySelected: (selectedDay, focusedDay) {
            setState(() {
              _selectedDay = selectedDay;
              _focusedDay = focusedDay;
            });
          },
        ),
        const Divider(),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 4.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'משמרות ליום זה (${selectedDayShifts.length})',
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
              TextButton.icon(
                onPressed: () {
                  widget.onAddManualShift(_selectedDay ?? DateTime.now());
                },
                icon: const Icon(Icons.add, size: 18),
                label: const Text('הוסף משמרת ידנית'),
              ),
            ],
          ),
        ),
        Expanded(
          child: selectedDayShifts.isEmpty
              ? const Center(
                  child: Text('אין משמרות רשומות בתאריך זה'),
                )
              : ListView.builder(
                  padding: const EdgeInsets.all(12),
                  itemCount: selectedDayShifts.length,
                  itemBuilder: (ctx, index) {
                    final shift = selectedDayShifts[index];
                    return Dismissible(
                      key: Key(shift.id),
                      direction: DismissDirection.endToStart,
                      background: Container(
                        alignment: Alignment.centerLeft,
                        padding: const EdgeInsets.only(left: 20),
                        color: Colors.red,
                        child: const Icon(Icons.delete, color: Colors.white),
                      ),
                      onDismissed: (direction) {
                        widget.onDeleteShift(shift.id);
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('המשמרת נמחקה')),
                        );
                      },
                      child: Card(
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                        margin: const EdgeInsets.symmetric(vertical: 6),
                        child: ListTile(
                          onTap: () => widget.onShiftClick(shift),
                          leading: CircleAvatar(
                            backgroundColor: Theme.of(context).colorScheme.primaryContainer,
                            child: const Icon(Icons.work_outline),
                          ),
                          title: Text('${shift.jobRole} • ₪${shift.netPay.toStringAsFixed(1)}',
                              style: const TextStyle(fontWeight: FontWeight.bold)),
                          subtitle: Text(
                            '${timeFormat.format(shift.startTime)} - ${shift.endTime != null ? timeFormat.format(shift.endTime!) : "..."} (${shift.netDurationHours.toStringAsFixed(1)} שעות)',
                          ),
                          trailing: const Icon(Icons.chevron_left),
                        ),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }
}
