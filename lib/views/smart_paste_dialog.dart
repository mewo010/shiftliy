import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/shift.dart';
import '../services/smart_parser.dart';

class SmartPasteDialog extends StatefulWidget {
  final double defaultHourlyRate;
  final Function(List<Shift>) onConfirmBatch;

  const SmartPasteDialog({
    super.key,
    required this.defaultHourlyRate,
    required this.onConfirmBatch,
  });

  @override
  State<SmartPasteDialog> createState() => _SmartPasteDialogState();
}

class _SmartPasteDialogState extends State<SmartPasteDialog> {
  final TextEditingController _textController = TextEditingController();
  List<Shift> _parsedShifts = [];

  final String sampleText = '''
07.09.2026 - 09:00 - 17:00 45 דקות + 50
07.09 - 22:00 - 06:00 ללא + 20.5
12.09.2026 - 16:00 - 00:30 30 דק' + 35
''';

  @override
  void initState() {
    super.initState();
    _textController.addListener(_onTextChanged);
  }

  void _onTextChanged() {
    setState(() {
      _parsedShifts = SmartParser.parseMultiple(
        _textController.text,
        defaultHourlyRate: widget.defaultHourlyRate,
      );
    });
  }

  @override
  void dispose() {
    _textController.removeListener(_onTextChanged);
    _textController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('dd.MM.yyyy');
    final timeFormat = DateFormat('HH:mm');

    return Directionality(
      textDirection: TextDirection.rtl,
      child: Dialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        child: Container(
          width: MediaQuery.of(context).size.width * 0.9,
          height: MediaQuery.of(context).size.height * 0.8,
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Expanded(
                    child: Row(
                      children: [
                        Icon(Icons.content_paste, color: Color(0xFF10B981)),
                        SizedBox(width: 8),
                        Flexible(
                          child: Text(
                            'הדבקה חכמה מ-WhatsApp',
                            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: () => Navigator.pop(context),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Expanded(
                    child: Text(
                      'פורמט: [תאריך] - [כניסה] - [יציאה] [הפסקה] + [טיפ]',
                      style: TextStyle(fontSize: 11, color: Colors.grey),
                      overflow: TextOverflow.ellipsis,
                      maxLines: 2,
                    ),
                  ),
                  TextButton(
                    onPressed: () {
                      _textController.text = sampleText.trim();
                    },
                    child: const Text('טקסט לדוגמה', style: TextStyle(fontSize: 12)),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              TextField(
                controller: _textController,
                maxLines: 4,
                decoration: const InputDecoration(
                  hintText: 'הדבק כאן את הטקסט החופשי...',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              Text(
                'תצוגה מקדימה (${_parsedShifts.length} משמרות זוהו)',
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Expanded(
                child: _parsedShifts.isEmpty
                    ? const Center(
                        child: Text('הדבק טקסט למעלה כדי לראות תצוגה מקדימה'),
                      )
                    : ListView.builder(
                        itemCount: _parsedShifts.length,
                        itemBuilder: (ctx, index) {
                          final shift = _parsedShifts[index];
                          return Card(
                            margin: const EdgeInsets.symmetric(vertical: 4),
                            child: ListTile(
                              title: Text(
                                '${dateFormat.format(shift.startTime)} | ${timeFormat.format(shift.startTime)} - ${shift.endTime != null ? timeFormat.format(shift.endTime!) : ""}',
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                              ),
                              subtitle: Text(
                                'הפסקה: ${shift.breakDurationMinutes} דק\' | טיפ: ₪${shift.tips.toStringAsFixed(1)} | נטו: ${shift.netDurationHours.toStringAsFixed(1)} שעות',
                                style: const TextStyle(fontSize: 12),
                              ),
                              trailing: Text(
                                '₪${shift.netPay.toStringAsFixed(1)}',
                                style: TextStyle(
                                  color: Theme.of(context).colorScheme.primary,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                          );
                        },
                      ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => Navigator.pop(context),
                      child: const Text('ביטול'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    flex: 2,
                    child: FilledButton.icon(
                      onPressed: _parsedShifts.isEmpty
                          ? null
                          : () {
                              widget.onConfirmBatch(_parsedShifts);
                              Navigator.pop(context);
                            },
                      icon: const Icon(Icons.download),
                      label: Text('הוסף ${_parsedShifts.length} משמרות'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
