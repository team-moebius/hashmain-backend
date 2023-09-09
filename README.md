# Moebius

## Definition
Moebius is a platform for supporting crypto currency trades conveniently, safely.

## Goal
* Supply flexible, automatic order functions (purchase & sale & stoploss) in trades.
* Supply easy-to-use UI/UX in mobile & pc through all kinds of web browser.
* Keep the latency of order execution under **100ms**.
* Support user Information management with email verification.

## Architecture & Technical specification
![Architecture](https://github.com/knunu/diagram/blob/master/Moebius%20architecture-Phase%204%20(latest).png?raw=false)
### Front Layer

1. Service Frontend
   * React, Redux-thunk, Axios, Material-ui, Typescript
   * Atomic design (Atom - Molecule - Organisms - Templates - Pages)
2. Data (Trade) Visualizer
   * kibana (ES visualizer), cerebro (ES monitor)

### API, Core Layer

1. Service Backend
   * Java 8, Spring boot 2, Webflux, [Reactor](https://projectreactor.io) (reactive stream, kafka, mongo driver), Spring security, Springfox-swagger2, jjwt, lombok
2. Trade Tracker
   * Same as Service Backend except Java 8 replaced with kotlin

### Data Layer

1. Main DB : MongoDB 4.0.12
2. Trade DB : ElasticSearch 7.1.1
3. Trade Message Queue : Kafka 2.0.1

## Sequence diagram of services
![Sequence diagram](https://user-images.githubusercontent.com/15190229/68497952-0ed03780-0299-11ea-9fd7-14406aa5d912.png)

## Vm options (backend)
```
-Dspring.profiles.active=${profile}
-Dreactor.netty.http.server.accessLogEnabled=true
-Xmx2048m
–Xms2048m
-XX:+UseG1GC
-XX:+DisableExplicitGC
-XX:+UseStringDeduplication
```
