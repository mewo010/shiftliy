import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/shift_provider.dart';

class TimerScreen extends StatefulWidget {
  final VoidCallback onOpenRateSettings;

  const TimerScreen({super.key, required this.onOpenRateSettings});

  @override
  State<TimerScreen> createState() => _TimerScreenState();
}

class _TimerScreenState extends State<TimerScreen> {
  String _selectedRole = 'מלצרות';
  bool _isBreakPaidPreference = false;

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<ShiftProvider>();
    final isRunning = provider.isRunning;
    final isBreak = provider.isBreakActive;
    final elapsed = provider.elapsed;
    final breakElapsed = provider.breakElapsed;

    final hours = elapsed.inHours.toString().padLeft(2, '0');
    final minutes = (elapsed.inMinutes % 60).toString().padLeft(2, '0');
    final seconds = (elapsed.inSeconds % 60).toString().padLeft(2, '0');
    final timerString = '$hours:$minutes:$seconds';

    final bMinutes = breakElapsed.inMinutes.toString().padLeft(2, '0');
    final bSeconds = (breakElapsed.inSeconds % 60).toString().padLeft(2, '0');
    final breakTimerString = '$bMinutes:$bSeconds';

    // Live calculation
    final unpaidBreakSec = (provider.activeShift?.isBreakPaid ?? _isBreakPaidPreference) ? 0 : breakElapsed.inSeconds;
    final netSec = (elapsed.inSeconds - unpaidBreakSec).clamp(0, double.maxFinite.toInt());
    final liveNetHours = netSec / 3600.0;
    final liveEstimatedPay = liveNetHours * provider.hourlyRate;

    final timeFormat = DateFormat('HH:mm');

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header Status & Rate
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              // Status Badge
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                decoration: BoxDecoration(
                  color: !isRunning
                      ? Theme.of(context).colorScheme.surfaceContainerHighest
                      : (isBreak ? const Color(0xFFFEF3C7) : const Color(0xFFD1FAE5)),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CircleAvatar(
                      radius: 5,
                      backgroundColor: !isRunning
                          ? Colors.grey
                          : (isBreak ? const Color(0xFFB45309) : const Color(0xFF047857)),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      !isRunning
                          ? 'לא פעיל'
                          : (isBreak ? 'בהפסקה ($breakTimerString)' : 'משמרת פעילה'),
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        color: !isRunning
                            ? Theme.of(context).colorScheme.onSurfaceVariant
                            : (isBreak ? const Color(0xFFB45309) : const Color(0xFF047857)),
                      ),
                    ),
                  ],
                ),
              ),

              // Hourly Rate Action Chip
              ActionChip(
                avatar: const Icon(Icons.edit, size: 14),
                label: Text('₪${provider.hourlyRate.toStringAsFixed(0)}/שעה'),
                onPressed: widget.onOpenRateSettings,
              ),
            ],
          ),

          const SizedBox(height: 16),

          // Main Timer Display Card
          Card(
            elevation: 2,
            shape: RoundedCornerShape(24),
            child: Padding(
              padding: const EdgeInsets.all(24.0),
              child: Column(
                children: [
                  if (isRunning && provider.activeShift != null) ...[
                    Text(
                      'תפקיד: ${provider.activeShift!.jobRole}',
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.primary,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text(
                      'החלה בשעה: ${timeFormat.format(provider.activeShift!.startTime)}',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const SizedBox(height: 12),
                  ] else ...[
                    Text(
                      'שעון נוכחות וטיימר משמרת',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 12),
                  ],

                  // Big Timer Digits
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(vertical: 24),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.primaryContainer.withOpacity(0.35),
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(
                        color: Theme.of(context).colorScheme.outlineVariant,
                      ),
                    ),
                    alignment: Alignment.center,
                    child: Text(
                      timerString,
                      style: TextStyle(
                        fontSize: 48,
                        fontWeight: FontWeight.bold,
                        fontFamily: 'monospace',
                        letterSpacing: 2,
                        color: isBreak
                            ? const Color(0xFFF59E0B)
                            : Theme.of(context).colorScheme.primary,
                      ),
                    ),
                  ),

                  const SizedBox(height: 16),

                  // Break Type Toggle Switch
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surfaceContainerHighest.withOpacity(0.4),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                (isRunning ? provider.activeShift?.isBreakPaid == true : _isBreakPaidPreference)
                                    ? 'הפסקה בתשלום'
                                    : 'הפסקה ללא תשלום',
                                style: const TextStyle(fontWeight: FontWeight.bold),
                              ),
                              Text(
                                (isRunning ? provider.activeShift?.isBreakPaid == true : _isBreakPaidPreference)
                                    ? 'זמן ההפסקה נספר בשכר השעתי'
                                    : 'זמן ההפסקה מקוזז מהשכר',
                                style: Theme.of(context).textTheme.bodySmall,
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 8),
                        Switch(
                          value: isRunning
                              ? (provider.activeShift?.isBreakPaid ?? false)
                              : _isBreakPaidPreference,
                          onChanged: (val) {
                            if (isRunning) {
                              provider.toggleBreakType(val);
                            } else {
                              setState(() {
                                _isBreakPaidPreference = val;
                              });
                            }
                          },
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          // Live Calculation Box
          Card(
            color: Theme.of(context).colorScheme.primaryContainer,
            shape: RoundedCornerShape(20),
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'שכר משוער למשמרת זו',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: Theme.of(context).colorScheme.onPrimaryContainer,
                        ),
                      ),
                      Text(
                        'לפי ₪${provider.hourlyRate.toStringAsFixed(0)}/שעה',
                        style: TextStyle(
                          fontSize: 12,
                          color: Theme.of(context).colorScheme.onPrimaryContainer.withOpacity(0.8),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '₪${liveEstimatedPay.toStringAsFixed(2)}',
                              style: TextStyle(
                                fontSize: 30,
                                fontWeight: FontWeight.w900,
                                color: Theme.of(context).colorScheme.primary,
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                            Text(
                              'שכר נטו משוער (ללא טיפים)',
                              style: TextStyle(
                                fontSize: 12,
                                color: Theme.of(context).colorScheme.onPrimaryContainer.withOpacity(0.7),
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(
                              '${liveNetHours.toStringAsFixed(2)} שעות',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                                color: Theme.of(context).colorScheme.onPrimaryContainer,
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                            Text(
                              'שעות עבודה נטו',
                              style: TextStyle(
                                fontSize: 12,
                                color: Theme.of(context).colorScheme.onPrimaryContainer.withOpacity(0.7),
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          // Role selection if not running
          if (!isRunning) ...[
            Card(
              shape: RoundedCornerShape(18),
              color: Theme.of(context).colorScheme.surfaceContainerHighest.withOpacity(0.4),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('בחר תפקיד למשמרת הקרובה:', style: TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      children: provider.availableJobRoles.take(4).map((role) {
                        return ChoiceChip(
                          label: Text(role),
                          selected: _selectedRole == role,
                          onSelected: (selected) {
                            if (selected) setState(() => _selectedRole = role);
                          },
                        );
                      }).toList(),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: () {
                provider.startShift(_selectedRole, _isBreakPaidPreference);
              },
              icon: const Icon(Icons.play_arrow, size: 28),
              label: const Text('התחל משמרת', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              style: FilledButton.styleFrom(
                backgroundColor: const Color(0xFF10B981),
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedCornerShape(16),
              ),
            ),
          ] else ...[
            // Controls when running
            Row(
              children: [
                Expanded(
                  child: isBreak
                      ? FilledButton.icon(
                          onPressed: provider.resumeFromBreak,
                          icon: const Icon(Icons.play_arrow),
                          label: const Text('חזור לעבודה'),
                          style: FilledButton.styleFrom(
                            backgroundColor: const Color(0xFF10B981),
                            padding: const EdgeInsets.symmetric(vertical: 14),
                            shape: RoundedCornerShape(16),
                          ),
                        )
                      : FilledButton.tonalIcon(
                          onPressed: provider.pauseForBreak,
                          icon: const Icon(Icons.coffee),
                          label: const Text('צא להפסקה'),
                          style: FilledButton.styleFrom(
                            padding: const EdgeInsets.symmetric(vertical: 14),
                            shape: RoundedCornerShape(16),
                          ),
                        ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton.icon(
                    onPressed: provider.stopShiftToReview,
                    icon: const Icon(Icons.stop),
                    label: const Text('סיום ועריכה'),
                    style: FilledButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedCornerShape(16),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            TextButton.icon(
              onPressed: provider.discardActiveShift,
              icon: const Icon(Icons.close, size: 16, color: Colors.red),
              label: const Text('בטל משמרת ללא שמירה', style: TextStyle(color: Colors.red, fontSize: 12)),
            ),
          ],
        ],
      ),
    );
  }

  RoundedRectangleBorder RoundedCornerShape(double r) =>
      RoundedRectangleBorder(borderRadius: BorderRadius.circular(r));
}
