import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';
import 'models/shift.dart';
import 'providers/shift_provider.dart';
import 'services/hive_storage_service.dart';
import 'views/timer_screen.dart';
import 'views/calendar_screen.dart';
import 'views/analytics_screen.dart';
import 'views/review_shift_sheet.dart';
import 'views/smart_paste_dialog.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await HiveStorageService.init();

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => ShiftProvider()),
      ],
      child: const ShiftlyApp(),
    ),
  );
}

class ShiftlyApp extends StatelessWidget {
  const ShiftlyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Shiftly',
      debugShowCheckedModeBanner: false,
      locale: const Locale('he', 'IL'),
      supportedLocales: const [
        Locale('he', 'IL'),
        Locale('en', 'US'),
      ],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF2563EB), // Shiftly Royal Blue
          brightness: Brightness.light,
        ),
        fontFamily: 'Roboto',
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF2563EB),
          brightness: Brightness.dark,
        ),
        fontFamily: 'Roboto',
      ),
      themeMode: ThemeMode.system,
      home: const ShiftlyHomeScreen(),
    );
  }
}

class ShiftlyHomeScreen extends StatefulWidget {
  const ShiftlyHomeScreen({super.key});

  @override
  State<ShiftlyHomeScreen> createState() => _ShiftlyHomeScreenState();
}

class _ShiftlyHomeScreenState extends State<ShiftlyHomeScreen> {
  int _currentIndex = 0;

  void _showRateSettingsDialog(BuildContext context, ShiftProvider provider) {
    final controller = TextEditingController(text: provider.hourlyRate.toStringAsFixed(0));
    showDialog(
      context: context,
      builder: (ctx) => Directionality(
        textDirection: TextDirection.rtl,
        child: AlertDialog(
          title: const Text('הגדרת תעריף שעה בסיסי'),
          content: TextField(
            controller: controller,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: const InputDecoration(
              labelText: 'תעריף לשעה (₪)',
              border: OutlineInputBorder(),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('ביטול'),
            ),
            FilledButton(
              onPressed: () {
                final rate = double.tryParse(controller.text);
                if (rate != null && rate > 0) {
                  provider.setHourlyRate(rate);
                }
                Navigator.pop(ctx);
              },
              child: const Text('שמור'),
            ),
          ],
        ),
      ),
    );
  }

  void _showSmartPasteDialog(BuildContext context, ShiftProvider provider) {
    showDialog(
      context: context,
      builder: (_) => SmartPasteDialog(
        defaultHourlyRate: provider.hourlyRate,
        onConfirmBatch: (imported) {
          provider.addParsedShifts(imported);
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('${imported.length} משמרות יובאו בהצלחה!')),
          );
        },
      ),
    );
  }

  void _openReviewSheet(BuildContext context, ShiftProvider provider, Shift shift) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => ReviewShiftSheet(
        shift: shift,
        availableRoles: provider.availableJobRoles,
        onSave: (updated) {
          provider.saveShift(updated);
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('המשמרת נשמרה בהצלחה')),
          );
        },
        onDiscard: () {
          provider.deleteShift(shift.id);
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('המשמרת נמחקה')),
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<ShiftProvider>();

    // Open review sheet if a shift just finished or is opened
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (provider.reviewingShift != null) {
        final shift = provider.reviewingShift!;
        provider.dismissReviewSheet();
        _openReviewSheet(context, provider, shift);
      }
    });

    final screens = [
      TimerScreen(
        onOpenRateSettings: () => _showRateSettingsDialog(context, provider),
      ),
      CalendarScreen(
        shifts: provider.shifts,
        onShiftClick: (shift) => _openReviewSheet(context, provider, shift),
        onDeleteShift: (id) => provider.deleteShift(id),
        onAddManualShift: (day) {
          final newShift = Shift(
            startTime: DateTime(day.year, day.month, day.day, 9, 0),
            endTime: DateTime(day.year, day.month, day.day, 17, 0),
            breakDurationMinutes: 30,
            hourlyRate: provider.hourlyRate,
            jobRole: 'מלצרות',
          );
          _openReviewSheet(context, provider, newShift);
        },
      ),
      AnalyticsScreen(shifts: provider.shifts),
    ];

    return Directionality(
      textDirection: TextDirection.rtl,
      child: LayoutBuilder(
        builder: (context, constraints) {
          final isWideScreen = constraints.maxWidth >= 720;

          return Scaffold(
            appBar: AppBar(
              title: const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Shiftly',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 22),
                  ),
                  Text(
                    'מעקב שעות ושכר חכם',
                    style: TextStyle(fontSize: 11, color: Colors.grey),
                  ),
                ],
              ),
              actions: [
                FilledButton.tonalIcon(
                  onPressed: () => _showSmartPasteDialog(context, provider),
                  icon: const Icon(Icons.content_paste, size: 16),
                  label: const Text('הדבקה מ-WhatsApp', style: TextStyle(fontSize: 12)),
                ),
                IconButton(
                  icon: const Icon(Icons.settings),
                  tooltip: 'הגדרת תעריף',
                  onPressed: () => _showRateSettingsDialog(context, provider),
                ),
                const SizedBox(width: 8),
              ],
            ),
            body: isWideScreen
                ? Row(
                    children: [
                      NavigationRail(
                        selectedIndex: _currentIndex,
                        onDestinationSelected: (index) =>
                            setState(() => _currentIndex = index),
                        labelType: NavigationRailLabelType.all,
                        destinations: const [
                          NavigationRailDestination(
                            icon: Icon(Icons.timer_outlined),
                            selectedIcon: Icon(Icons.timer),
                            label: Text('טיימר'),
                          ),
                          NavigationRailDestination(
                            icon: Icon(Icons.calendar_month_outlined),
                            selectedIcon: Icon(Icons.calendar_month),
                            label: Text('יומן והיסטוריה'),
                          ),
                          NavigationRailDestination(
                            icon: Icon(Icons.bar_chart_outlined),
                            selectedIcon: Icon(Icons.bar_chart),
                            label: Text('אנליטיקה'),
                          ),
                        ],
                      ),
                      const VerticalDivider(thickness: 1, width: 1),
                      Expanded(child: screens[_currentIndex]),
                    ],
                  )
                : screens[_currentIndex],
            bottomNavigationBar: isWideScreen
                ? null
                : NavigationBar(
                    selectedIndex: _currentIndex,
                    onDestinationSelected: (index) =>
                        setState(() => _currentIndex = index),
                    destinations: const [
                      NavigationDestination(
                        icon: Icon(Icons.timer_outlined),
                        selectedIcon: Icon(Icons.timer),
                        label: 'טיימר',
                      ),
                      NavigationDestination(
                        icon: Icon(Icons.calendar_month_outlined),
                        selectedIcon: Icon(Icons.calendar_month),
                        label: 'יומן והיסטוריה',
                      ),
                      NavigationDestination(
                        icon: Icon(Icons.bar_chart_outlined),
                        selectedIcon: Icon(Icons.bar_chart),
                        label: 'אנליטיקה',
                      ),
                    ],
                  ),
          );
        },
      ),
    );
  }
}
