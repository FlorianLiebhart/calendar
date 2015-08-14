# Calendar - A collaborative calendar

#### by [mychris](https://github.com/mychris), [FlorianLiebhart](https://github.com/FlorianLiebhart/), [simonkaltenbacher](https://github.com/simonkaltenbacher)
--------

## Development

### Getting started

1. Check out this git respository: 
```git checkout git@github.com:simonkaltenbacher/calendar.git```
2. Download and install [PostgreSQL](http://www.postgresql.org/download/) 
3. Run the psql-command line: ```psql```
   * Create a database in the psql interactive: ```CREATE DATABASE calendar;``` 
   * Create a user with a password for the database: ```CREATE USER myusername WITH PASSWORD 'mypassword';```  
   **NOTE**: Have a look at the entries starting with `db.default.*` in `application.conf`.  
   The user name and password declared there must match the one you created in the psql interactive.
4. Run the application with ```sbt run``` (it will then be running on the standard port 9000).
   * Open the application in the browser (perferably Google Chrome) with the URL `localhost:9000`
   * Register with user name and password on the login page (does not have to match the one specified for the database).
   * You can now log in to the calendar.


### Running Tests

Run ```sbt test``` from the command line to run the tests.



## Technology stack

### Build Tool
- [SBT v. 0.13.0](http://www.scala-sbt.org/0.13.0/docs/home.html) [`open source`](https://github.com/sbt/sbt)
  * easy to use build tool for Scala, Java, and more. 

### DBS 
- [PostgreSQL](http://www.postgresql.org/) [`open source`](https://github.com/postgres/postgres)
  * object-relational database management system.
- [Slick v. 2.0.1](http://slick.typesafe.com/) [`open source`](https://github.com/slick/slick)
  * modern database query and access library for Scala.
  * [Documentation](http://slick.typesafe.com/doc/2.0.1/)
  * [ScalaDoc](http://slick.typesafe.com/doc/2.0.1/api/#package)
  * [GitHub](https://github.com/slick/slick)

### Backend
- [Play Framework v. 2.2.2](http://www.playframework.com/) [`open source`](https://github.com/playframework/playframework)
  * scalable, lightweight, stateless, developer-friendly web framework for Java and Scala.
  * [Documentation](http://www.playframework.com/documentation/2.2.x/Home)
- [Scala v. 2.10.3](http://www.scala-lang.org/) [`open source`](https://github.com/scala/scala)
  * modern object-oriented, functional programming language.
  * [API Documenation](http://www.scala-lang.org/api/2.10.3/#package)
- [Akka v. 2.2.4](http://akka.io/) [`open source`](https://github.com/akka/akka)
  * toolkit and runtime for building highly concurrent, distributed, and fault tolerant event-driven applications on the JVM.
  * [Scala documentation](http://doc.akka.io/docs/akka/2.2.4/scala.html)
   * [API documentation](http://doc.akka.io/api/akka/2.2.4/#package)

### Frontend
- [JQuery v. 2.1.0](http://jquery.com/) [`open source`](https://github.com/jquery/jquery)
  * fast, small, and feature-rich JavaScript library.
  * [API documentation](http://api.jquery.com/)
- [Bootstrap v. 3.1.1](http://getbootstrap.com/) [`open source`](https://github.com/twbs/bootstrap)
  * popular front-end framework for developing responsive, mobile first projects on the web.
- [FullCalendar v2beta](http://arshaw.com/fullcalendar/) [`open source`](https://github.com/arshaw/fullcalendar)
  * jQuery plugin that provides a full-sized, drag & drop calendar.
  * [Documentation](http://arshaw.com/fullcalendar/docs2/)
- [D3 v. 3.4.6](http://d3js.org/) [`open source`](https://github.com/mbostock/d3/)
  * JavaScript library for manipulating documents based on data. D3 helps bring data to life using HTML, SVG and CSS.
- [Eonastan bootstrap-datetimepicker](http://eonasdan.github.io/bootstrap-datetimepicker/) [`open source`](https://github.com/Eonasdan/bootstrap-datetimepicker)
  * date/time picker widget for Twitter Bootstrap v3.
- [selectize.js v. 0.8.5](http://brianreavis.github.io/selectize.js/) [`open source`](https://github.com/brianreavis/selectize.js)
  * jQuery-based hybrid of a textbox and select-box.
  * [Documentation](https://github.com/brianreavis/selectize.js/blob/master/docs/usage.md)
  * [API documentation](https://github.com/brianreavis/selectize.js/blob/master/docs/api.md)


## Main Features
- Freie Zeitslots für Termine finden
    - Angabe von:
      - Beteiligte Personen
      - Dauer
      - Evtl. Zeitrahmen (Nachmittags/...)
    - Ausgabe von Terminvorschlägen, an dem alle teilnehmen können.
      - Davon kann man dann einen auswählen.
      - Zwischen zwei Terminen einen Zeitslot anzeigen, keine Termine (sodass nicht für jede Minute ein Termin angezeigt wird)
- Termin als Vorschlag markieren (reserviert), Termin als fest markieren.
- Kategorie-Tagging (mit Priorität) eines Termins, welcher eine Standardpriorität (wichtig für Konfliktlösung) beinhaltet  
  (Beispieltags: Uni:3, Arbeit:1, Privat:2,..), mit jeweils verschiedener Standard-Priorität
- Möglichkeit des Eintragens kollidierender Termine
- Halbautomatisches Auflösen von Konflikten (kollidierende Termine) 
- GUI: Kalendaransicht


# Notes
1. The tables of the ProposalDataAccessService are not created at the moment. Support will be added as soon as the component's implementation is finished.
2. There seems to be a bug in slick regarding compound primary keys. Therefore the compound keys are commented out in CalendarDataAccessService.
