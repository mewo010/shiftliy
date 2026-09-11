import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  final FlutterLocalNotificationsPlugin _notificationsPlugin =
      FlutterLocalNotificationsPlugin();

  static const int shiftNotificationId = 1001;
  static const String channelId = 'shiftly_live_shift_channel';
  static const String channelName = 'Shiftly Live Shift Tracker';
  static const String channelDescription =
      'Shows live running shift status and quick controls';

  bool _isInitialized = false;

  Future<void> init({Function(String?)? onActionSelected}) async {
    if (kIsWeb || _isInitialized) return;

    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');

    const darwinSettings = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: false,
    );

    const linuxSettings = LinuxInitializationSettings(
      defaultActionName: 'Open notification',
    );

    const initSettings = InitializationSettings(
      android: androidSettings,
      iOS: darwinSettings,
      macOS: darwinSettings,
      linux: linuxSettings,
    );

    try {
      await _notificationsPlugin.initialize(
        initSettings,
        onDidReceiveNotificationResponse: (NotificationResponse response) {
          if (onActionSelected != null) {
            onActionSelected(response.actionId ?? response.payload);
          }
        },
      );

      // Create Android Notification Channel
      final androidPlatform = _notificationsPlugin
          .resolvePlatformSpecificImplementation<
              AndroidFlutterLocalNotificationsPlugin>();
      if (androidPlatform != null) {
        await androidPlatform.createNotificationChannel(
          const AndroidNotificationChannel(
            channelId,
            channelName,
            description: channelDescription,
            importance: Importance.low, // Ongoing notification without chime
            showBadge: false,
          ),
        );
        await androidPlatform.requestNotificationsPermission();
      }

      _isInitialized = true;
    } catch (e) {
      debugPrint('NotificationService init error: $e');
    }
  }

  Future<void> showShiftNotification({
    required String title,
    required String body,
    required bool isBreak,
  }) async {
    if (kIsWeb || !_isInitialized) return;

    final androidDetails = AndroidNotificationDetails(
      channelId,
      channelName,
      channelDescription: channelDescription,
      importance: Importance.low,
      priority: Priority.low,
      ongoing: true,
      autoCancel: false,
      showWhen: true,
      actions: [
        AndroidNotificationAction(
          isBreak ? 'resume_action' : 'break_action',
          isBreak ? 'חזור מהפסקה' : 'יציאה להפסקה',
          showsUserInterface: true,
        ),
        const AndroidNotificationAction(
          'stop_action',
          'סיום משמרת',
          showsUserInterface: true,
          cancelNotification: false,
        ),
      ],
    );

    const darwinDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: false,
      presentSound: false,
    );

    final notificationDetails = NotificationDetails(
      android: androidDetails,
      iOS: darwinDetails,
      macOS: darwinDetails,
    );

    try {
      await _notificationsPlugin.show(
        shiftNotificationId,
        title,
        body,
        notificationDetails,
      );
    } catch (e) {
      debugPrint('Failed to show notification: $e');
    }
  }

  Future<void> cancelShiftNotification() async {
    if (kIsWeb || !_isInitialized) return;
    try {
      await _notificationsPlugin.cancel(shiftNotificationId);
    } catch (e) {
      debugPrint('Failed to cancel notification: $e');
    }
  }
}
