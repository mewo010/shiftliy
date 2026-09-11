import 'package:flutter/material.dart';
import 'package:intl/intl.dart' hide TextDirection;
import '../models/shift.dart';
import '../models/expense.dart';

class ReviewShiftSheet extends StatefulWidget {
  final Shift shift;
  final List<String> availableRoles;
  final Function(Shift) onSave;
  final VoidCallback onDiscard;

  const ReviewShiftSheet({
    super.key,
    required this.shift,
    required this.availableRoles,
    required this.onSave,
    required this.onDiscard,
  });

  @override
  State<ReviewShiftSheet> createState() => _ReviewShiftSheetState();
}

class _ReviewShiftSheetState extends State<ReviewShiftSheet> {
  late DateTime _startTime;
  late DateTime _endTime;
  late int _breakMinutes;
  late bool _isBreakPaid;
  late double _tips;
  late double _hourlyRate;
  late String _jobRole;
  late String _notes;
  late List<Expense> _expenses;

  final TextEditingController _tipsController = TextEditingController();
  final TextEditingController _breakController = TextEditingController();
  final TextEditingController _notesController = TextEditingController();

  final timeFormat = DateFormat('HH:mm');
  final dateFormat = DateFormat('dd.MM.yyyy');

  @override
  void initState() {
    super.initState();
    _startTime = widget.shift.startTime;
    _endTime = widget.shift.endTime ?? DateTime.now();
    _breakMinutes = widget.shift.breakDurationMinutes;
    _isBreakPaid = widget.shift.isBreakPaid;
    _tips = widget.shift.tips;
    _hourlyRate = widget.shift.hourlyRate;
    _jobRole = widget.shift.jobRole;
    _notes = widget.shift.notes;
    _expenses = List.from(widget.shift.expenses);

    _tipsController.text = _tips > 0 ? _tips.toString() : '';
    _breakController.text = _breakMinutes.toString();
    _notesController.text = _notes;
  }

  @override
  void dispose() {
    _tipsController.dispose();
    _breakController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  Shift get _currentShift {
    return Shift(
      id: widget.shift.id,
      startTime: _startTime,
      endTime: _endTime,
      breakDurationMinutes: _breakMinutes,
      isBreakPaid: _isBreakPaid,
      hourlyRate: _hourlyRate,
      jobRole: _jobRole,
      tips: _tips,
      notes: _notes,
      expenses: _expenses,
    );
  }

  void _showAddExpenseDialog() {
    String title = '';
    double amount = 0.0;
    String category = 'נסיעות';

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('הוסף הוצאה למשמרת'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              decoration: const InputDecoration(labelText: 'תיאור (למשל מונית, אוכל)'),
              onChanged: (val) => title = val,
            ),
            const SizedBox(height: 8),
            TextField(
              decoration: const InputDecoration(labelText: 'סכום (₪)'),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              onChanged: (val) => amount = double.tryParse(val) ?? 0.0,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('ביטול'),
          ),
          FilledButton(
            onPressed: () {
              if (title.isNotEmpty && amount > 0) {
                setState(() {
                  _expenses.add(Expense(title: title, amount: amount, category: category));
                });
                Navigator.pop(ctx);
              }
            },
            child: const Text('הוסף'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final computed = _currentShift;

    return Directionality(
      textDirection: TextDirection.rtl,
      child: Container(
        padding: EdgeInsets.only(
          top: 20,
          left: 20,
          right: 20,
          bottom: MediaQuery.of(context).viewInsets.bottom + 20,
        ),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surface,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
        ),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'סיכום ועריכת משמרת',
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: () => Navigator.pop(context),
                  ),
                ],
              ),

              const SizedBox(height: 12),

              // Summary Highlight Card
              Card(
                color: Theme.of(context).colorScheme.primaryContainer,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('שכר נטו סופי', style: Theme.of(context).textTheme.labelMedium),
                          Text(
                            '₪${computed.netPay.toStringAsFixed(1)}',
                            style: TextStyle(
                              fontSize: 26,
                              fontWeight: FontWeight.w900,
                              color: Theme.of(context).colorScheme.primary,
                            ),
                          ),
                        ],
                      ),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text('שעות נטו', style: Theme.of(context).textTheme.labelMedium),
                          Text(
                            '${computed.netDurationHours.toStringAsFixed(2)} שעות',
                            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),

              const SizedBox(height: 16),

              // Time & Date Pickers
              Row(
                children: [
                  Expanded(
                    child: ListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text('כניסה', style: TextStyle(fontSize: 12)),
                      subtitle: Text(
                        '${dateFormat.format(_startTime)} ${timeFormat.format(_startTime)}',
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                  ),
                  Expanded(
                    child: ListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text('יציאה', style: TextStyle(fontSize: 12)),
                      subtitle: Text(
                        '${dateFormat.format(_endTime)} ${timeFormat.format(_endTime)}',
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 12),

              // Break Duration & Break Type
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _breakController,
                      decoration: const InputDecoration(
                        labelText: 'הפסקה (בדקות)',
                        border: OutlineInputBorder(),
                      ),
                      keyboardType: TextInputType.number,
                      onChanged: (val) {
                        setState(() {
                          _breakMinutes = int.tryParse(val) ?? 0;
                        });
                      },
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        border: Border.all(color: Theme.of(context).colorScheme.outlineVariant),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Flexible(
                            child: Text(
                              'הפסקה בתשלום',
                              style: TextStyle(fontSize: 12),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          Switch(
                            value: _isBreakPaid,
                            onChanged: (val) => setState(() => _isBreakPaid = val),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 12),

              // Tips Input
              TextField(
                controller: _tipsController,
                decoration: const InputDecoration(
                  labelText: 'טיפים נוספים (₪)',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.monetization_on),
                ),
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                onChanged: (val) {
                  setState(() {
                    _tips = double.tryParse(val) ?? 0.0;
                  });
                },
              ),

              const SizedBox(height: 12),

              // Job Role Selector
              DropdownButtonFormField<String>(
                value: _jobRole,
                decoration: const InputDecoration(
                  labelText: 'תפקיד',
                  border: OutlineInputBorder(),
                ),
                items: widget.availableRoles.map((role) {
                  return DropdownMenuItem(value: role, child: Text(role));
                }).toList(),
                onChanged: (val) {
                  if (val != null) setState(() => _jobRole = val);
                },
              ),

              const SizedBox(height: 16),

              // Expenses Section
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'הוצאות שקוזזו (${_expenses.length})',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  TextButton.icon(
                    onPressed: _showAddExpenseDialog,
                    icon: const Icon(Icons.add, size: 16),
                    label: const Text('הוסף הוצאה'),
                  ),
                ],
              ),

              if (_expenses.isNotEmpty)
                ..._expenses.map((exp) => ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(Icons.receipt, size: 18),
                      title: Text(exp.title),
                      trailing: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text('-₪${exp.amount.toStringAsFixed(1)}', style: const TextStyle(color: Colors.red)),
                          IconButton(
                            icon: const Icon(Icons.delete, size: 18),
                            onPressed: () {
                              setState(() {
                                _expenses.remove(exp);
                              });
                            },
                          ),
                        ],
                      ),
                    )),

              const SizedBox(height: 16),

              // Bottom Actions
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () {
                        widget.onDiscard();
                        Navigator.pop(context);
                      },
                      style: OutlinedButton.styleFrom(
                        foregroundColor: Colors.red,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                      ),
                      child: const Text('מחק'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    flex: 2,
                    child: FilledButton(
                      onPressed: () {
                        widget.onSave(_currentShift);
                        Navigator.pop(context);
                      },
                      style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 14),
                      ),
                      child: const Text('שמור משמרת'),
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
