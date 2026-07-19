import 'package:flutter_local_notifications/flutter_local_notifications.dart';

/// Local (on-device) notifications for high-activity alerts. No server, no push
/// tokens — scheduled and fired entirely on the phone.
class NotificationService {
  NotificationService([FlutterLocalNotificationsPlugin? plugin])
      : _plugin = plugin ?? FlutterLocalNotificationsPlugin();

  final FlutterLocalNotificationsPlugin _plugin;

  static const _channel = AndroidNotificationDetails(
    'mozzid_activity',
    'Mosquito activity',
    channelDescription: 'Alerts for high vector activity near you.',
    importance: Importance.high,
    priority: Priority.high,
  );

  Future<void> init() async {
    const android = AndroidInitializationSettings('@mipmap/ic_launcher');
    const ios = DarwinInitializationSettings();
    await _plugin.initialize(
      const InitializationSettings(android: android, iOS: ios),
    );
  }

  Future<void> requestPermission() async {
    await _plugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
    await _plugin
        .resolvePlatformSpecificImplementation<
            IOSFlutterLocalNotificationsPlugin>()
        ?.requestPermissions(alert: true, badge: true, sound: true);
  }

  Future<void> showActivityAlert(String title, String body) => _plugin.show(
        DateTime.now().millisecondsSinceEpoch ~/ 1000,
        title,
        body,
        const NotificationDetails(android: _channel, iOS: DarwinNotificationDetails()),
      );
}
