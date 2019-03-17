# WhosInBot - Clojure

[![Build Status](https://travis-ci.org/tonylpt/WhosInBot-Clojure.svg?branch=master)](https://travis-ci.org/tonylpt/WhosInBot-Clojure)
[![codecov](https://img.shields.io/codecov/c/github/tonylpt/WhosInBot-Clojure.svg)](https://codecov.io/gh/tonylpt/WhosInBot-Clojure)

This is an implementation of the [WhosInBot](https://github.com/col/whos_in_bot) in Clojure.

Check out the [Scala](https://github.com/tonylpt/WhosInBot-Scala) and [Rust](https://github.com/tonylpt/WhosInBot-Rust) versions.


## Usage
An instance of this Telegram bot has been deployed and running. To start using, simply add `@whosincljbot` to a group chat and type `/start_roll_call`. 

Refer to the original [WhosInBot](https://github.com/col/whos_in_bot/blob/master/README.md) for the full usage description.

### Basic Commands
- `/start_roll_call` - Start a new roll call
- `/start_roll_call Some cool title` - Start a new roll call with a title
- `/set_title Some cool title` - Add a title to the current roll call
- `/end_roll_call` - End the current roll call

### Attendance Commands
- `/in` - Let everyone know you'll be attending
- `/in Some random comment` - Let everyone know you'll be attending, with a comment
- `/out` - Let everyone know you won't be attending
- `/out Some excuses` - Let everyone know you won't be attending, with a comment
- `/maybe` - Let everyone know that you might be coming
- `/maybe Erm..` - Let everyone know that you might be coming, with a comment
- `/set_in_for Dave` - Let everyone know that Dave will be attending (with an optional comment)
- `/set_out_for Dave` - Let everyone know that Dave won't be attending (with an optional comment)
- `/set_maybe_for Dave` - Let everyone know that Dave might be coming (with an optional comment)
- `/whos_in` - List attendees

### Other Commands
- `/shh` - Tells WhosInBot not to list all attendees after every response
- `/louder` - Tells WhosInBot to list all attendees after every response


## Development

### Prerequisites
- [Leiningen](https://leiningen.org/#install)
- [Docker Compose](https://docs.docker.com/compose/install/)

### Setup
1. [Create a Telegram bot](https://core.telegram.org/bots#creating-a-new-bot) for development and obtain the authorization token.
2. Copy `resources/config.template-dev.edn` to `resources/config.edn` and fill in the Telegram token.        
3. Start the development PostgreSQL and Redis with Docker Compose:

        docker-compose up -d
        
   This automatically creates `whosin_dev` and `whosin_test` databases.
   
### Run with Leiningen
1. Apply dev database migrations:

        lein migrate
        
2. Apply test database migrations:

        ENVIRONMENT=test lein migrate
        
3. Run tests:

        lein test
        
4. Run the app locally:

        lein run
        

### Run from JAR
1. Make sure `resources/config.edn` has the correct values.
2. Build the JAR:

        lein uberjar

    The standalone JAR will be generated at `target/uberjar/whosin-standalone.jar`.
    
3. Apply database migrations:
       
        java -jar target/uberjar/whosin-standalone.jar --migrate
 
4. Run the app:        
       
        java -jar target/uberjar/whosin-standalone.jar
        

## Deployment
### Notes
* A basic CI/CD pipeline with [Travis CI](https://travis-ci.org) and [Heroku](https://www.heroku.com) (Worker Dyno) is included.
* PostgreSQL 9.6 and above is recommended.
* Redis is required for rate limiting incoming messages.

### Setup
1. [Create a Telegram bot](https://core.telegram.org/bots#creating-a-new-bot) for production and obtain the authorization token.
2. __Optional:__ Create a project on [Sentry](https://sentry.io) and obtain the DSN.
3. Create a project on [Heroku](https://www.heroku.com) and add `TELEGRAM_TOKEN` and `SENTRY_DSN` as Config Vars with values from step 1 and 2.
4. Provision PostgreSQL and Redis on Heroku.
5. Obtain the Heroku API key from [Account Settings](https://dashboard.heroku.com/account).
6. Set up the project on [Travis CI](https://travis-ci.org) and add `HEROKU_API_KEY` and `HEROKU_APP_NAME` as Environment Variables.
7. Trigger a new build and deployment from Travis.
8. Once the deployment completes, go to Heroku Dashboard and enable the Worker Dyno under _Resources_.
