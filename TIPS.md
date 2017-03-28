# TIPS

### Fast generate UUIDs
 
```bash
$ ruby -r 'securerandom' -e 'puts SecureRandom.uuid'
9ebb963e-69fc-4ce3-88ce-4b8863e7a971
```

### Run API calls from console

```bash
$ curl -H "Cookie: user_id=9ebb963e-69fc-4ce3-88ce-4b8863e7a971" localhost:8888/api/v1/notifications
```

### Insert notifications in database

```bash
$ ruby script/send_notification.rb
```

### Set mysql timezone to UTC

```bash
$ mysql -u root
> SET GLOBAL time_zone = '+3:00';
```
