#Backend Scala Sample

This project serves as the Scala work sample for Backend Engineer position.

## Installation

This application requires MySQL server running. The easiest setup requires docker to be installed. You can get a copy by installing [Docker Toolbox](https://docs.docker.com/engine/installation/).

Then, make sure you have commands `docker`, `docker-machine` and `docker-compose` installed.

```bash
$ # Create a docker machine and start it
$ docker-machine create --driver virtualbox default

$ # Check ip configuration for this machine. For me it is 192.168.99.100
$ # this ip is required below
$ docker-machine env default

$ # Copy environment information
$ cp .env.sample .env
$ # Edit .env, replacing 192.168.99.100 with docker machine ip, if needed

$ # Load environment, this should be done in every tab where sbt is used
$ source .env

$ # Start mysql
$ docker-compose up

$ # Create and migrate databases
$ sbt flywayMigrate
$ mysql -u root -h 192.168.99.100 -p123456 -e "CREATE DATABASE backend_scala_app_test; GRANT ALL PRIVILEGES on backend_scala_app_test.* to 'dev'@'%';"
$ ./script/migrate_db_test.sh

$ # Start sbt
$ sbt
> // Run server
> run

> // Run tests
> test
```

## Components

There are two components of this service.

### API

All external communication goes through the API. Authentication and authorization is mocked in HasUserFilter and it does not fit the purpose of this sample test. If you want to act as user `x` then set cookie `user_id` to value `x`.

The API is implemented using [Finatra](https://github.com/twitter/finatra) API and can be found in `src/main/scala/com/kuende/backendapp/api`.

### Pubsub

Communication between services is implemented asynchronously using [Google Cloud Pubsub](https://cloud.google.com/pubsub/docs/overview) push [endpoints](https://cloud.google.com/pubsub/docs/subscriber#receive_push). Other services push messages into a queue defined by a consumer. Messages are then sent to the webhooks define by the consumers.

### Persistence layer

Data is persisted in MySQL using [Quill](getquill.io) using the finagle mysql driver.

## Project description

This service implements Notification Service, a microservice which manages notifications for users. Project flow looks like this:

- [X] notifications are created using Google Cloud Pubsub webhook at `/_pubsub/task/main.notifications.publish`
- [X] users can view their own notifications using the API `/api/v1/notifications`

## Tasks

You are required to implemented the functionality listed below. All features need tests as well, written using scalatest framework.

### Notification API

Currently there is an API implemented which lists all notifications for an user. Please add this functionality:

- Currently all notifications are returned for every request. Some users can have thousands of notifications. Implement pagination.
- Get all notifications since a date sent as parameter `since`
- Return all unread notifications (parameter `unseen=true`)

Note: You can use the `scripts/publish-notification.rb` to simulate Cloud PubSub webhook calls.

### Inter-Service communication

- Old notifications must be deleted once in a while. Create a new pubsub endpoint which will remove unread notifications older than 7 days.

### Advanced API usage

- Implement a PUT `/notifications/:id` API which marks a notification as seen.
- Return the number of unseen notifications for the GET Notifications API as `X-Notifications-Unseen` header.

## TIPS

You may need this tips while developing the sample.

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
> SET GLOBAL time_zone = '0:00';
```
