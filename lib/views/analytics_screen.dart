import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/shift.dart';

class AnalyticsScreen extends StatefulWidget {
  final List<Shift> shifts;

  const AnalyticsScreen({super.key, required this.shifts});

  @override
  State<AnalyticsScreen> createState() => _AnalyticsScreenState();
}

class _AnalyticsScreenState extends State<AnalyticsScreen> {
  DateTime _selectedMonth = DateTime(DateTime.now().year, DateTime.now().month, 1);

  @override
  Widget build(BuildContext context) {
    final filteredShifts = widget.shifts.where((s) {
      return s.startTime.year == _selectedMonth.year && s.startTime.month == _selectedMonth.month;
    }).toList();

    final totalHours = filteredShifts.fold(0.0, (sum, s) => sum + s.netDurationHours);
    final totalGross = filteredShifts.fold(0.0, (sum, s) => sum + s.grossPay);
    final totalTips = filteredShifts.fold(0.0, (sum, s) => sum + s.tips);
    final totalExpenses = filteredShifts.fold(0.0, (sum, s) => sum + s.totalExpenses);
    final totalNet = totalGross + totalTips - totalExpenses;

    // Role breakdown
    final roleMap = <String, double>{};
    for (var s in filteredShifts) {
      roleMap[s.jobRole] = (roleMap[s.jobRole] ?? 0.0) + s.netPay;
    }

    final monthFormat = DateFormat('MMMM yyyy', 'he_IL');

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Month Selector
          Card(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  IconButton(
                    icon: const Icon(Icons.arrow_forward),
                    onPressed: () {
                      setState(() {
                        _selectedMonth = DateTime(_selectedMonth.year, _selectedMonth.month + 1, 1);
                      });
                    },
                  ),
                  Text(
                    monthFormat.format(_selectedMonth),
                    style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  IconButton(
                    icon: const Icon(Icons.arrow_back),
                    onPressed: () {
                      setState(() {
                        _selectedMonth = DateTime(_selectedMonth.year, _selectedMonth.month - 1, 1);
                      });
                    },
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          // Total Net Card Hero
          Card(
            color: Theme.of(context).colorScheme.primaryContainer,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                children: [
                  const Text('שכר נטו סופי לחודש זה', style: TextStyle(fontSize: 14)),
                  const SizedBox(height: 6),
                  Text(
                    '₪${totalNet.toStringAsFixed(1)}',
                    style: TextStyle(
                      fontSize: 36,
                      fontWeight: FontWeight.w900,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '${filteredShifts.length} משמרות בחודש זה',
                    style: TextStyle(fontSize: 12, color: Theme.of(context).colorScheme.onPrimaryContainer),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          // Metric Grid
          Row(
            children: [
              Expanded(
                child: _buildMetricCard(
                  title: 'שכר ברוטו',
                  value: '₪${totalGross.toStringAsFixed(1)}',
                  icon: Icons.trending_up,
                  color: Theme.of(context).colorScheme.primary,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildMetricCard(
                  title: 'סה״כ טיפים',
                  value: '₪${totalTips.toStringAsFixed(1)}',
                  icon: Icons.monetization_on,
                  color: const Color(0xFF10B981),
                ),
              ),
            ],
          ),

          const SizedBox(height: 12),

          Row(
            children: [
              Expanded(
                child: _buildMetricCard(
                  title: 'הוצאות שקוזזו',
                  value: '-₪${totalExpenses.toStringAsFixed(1)}',
                  icon: Icons.receipt_long,
                  color: Colors.redAccent,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildMetricCard(
                  title: 'שעות עבודה נטו',
                  value: '${totalHours.toStringAsFixed(1)} שעות',
                  icon: Icons.access_time,
                  color: Colors.deepPurple,
                ),
              ),
            ],
          ),

          const SizedBox(height: 20),

          // Role Breakdown
          Card(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('פילוח הכנסות לפי תפקיד', style: TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 12),
                  if (roleMap.isEmpty)
                    const Text('אין נתונים לחודש זה', style: TextStyle(color: Colors.grey))
                  else
                    ...roleMap.entries.map((entry) {
                      final ratio = totalNet > 0 ? (entry.value / totalNet).clamp(0.0, 1.0) : 0.0;
                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 6.0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(entry.key, style: const TextStyle(fontWeight: FontWeight.bold)),
                                Text('₪${entry.value.toStringAsFixed(0)}'),
                              ],
                            ),
                            const SizedBox(height: 4),
                            LinearProgressIndicator(
                              value: ratio,
                              minHeight: 8,
                              borderRadius: BorderRadius.circular(4),
                            ),
                          ],
                        ),
                      );
                    }),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMetricCard({
    required String title,
    required String value,
    required IconData icon,
    required Color color,
  }) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 18, color: color),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    title,
                    style: const TextStyle(fontSize: 12, color: Colors.grey),
                    overflow: TextOverflow.ellipsis,
                    maxLines: 1,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              value,
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: color),
              overflow: TextOverflow.ellipsis,
              maxLines: 1,
            ),
          ],
        ),
      ),
    );
  }
}
