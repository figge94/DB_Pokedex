# Pokemon Battler

Pokemon Battler är en konsolbaserad Java-applikation där användaren kan hantera sin egen Pokedex, strida mot datorstyrda motståndare och följa sin stridsstatistik.

Projektet är utvecklat i Java 21 och använder PostgreSQL, JPA/Hibernate och MongoDB för databashantering. JSON-filhantering med Jackson finns också kvar som en del av projektet.

## Tekniker

**Java 21** – programmeringsspråk

**Maven** – beroendehantering och byggverktyg

**PostgreSQL** – relationsdatabas för Pokemon-data

**JPA/Hibernate** – objektorienterad åtkomst till PostgreSQL

**MongoDB** – dokumentdatabas för stridsstatistik och stridsloggar

**MongoDB** Java Sync Driver – kommunikation med MongoDB

**Jackson** – serialisering och deserialisering av JSON

**Docker Compose** – lokal databasmiljö

**dotenv-java** – hantering av miljövariabler

**JUnit** – enhetstester

## Kom igång
- Förutsättningar

Du behöver ha följande installerat:

- Java 21
- Maven
- Docker Desktop eller Docker Engine med Compose
- IntelliJ IDEA eller motsvarande IDE

### 1. Klona projektet:
```
git clone https://github.com/figge94/DB_Pokedex.git
cd DB_Pokedex
```
### 2. Konfigurera miljövariabler

Kopiera .env.example till .env och ange dina egna databasuppgifter.

.env används lokalt och ska inte versionshanteras. .env.example innehåller endast exempelvärden.

Observera att MongoDB-användarnamnet och lösenordet för närvarande inte används av Java-anslutningen. Den lokala MongoDB-konfigurationen utgår från en anslutning utan autentisering.


### 3. Starta databaserna

Projektet använder PostgreSQL och MongoDB.

MongoDB körs lokalt med Docker och nås via port 27018. PostgreSQL nås via port 5433.

Starta tjänsterna som definieras i projektets Docker Compose-konfiguration:

```
docker compose up -d
```

Kontrollera vilka containrar som körs:

```
docker compose ps
```

PostgreSQL måste också vara startad och konfigurerad med de uppgifter som anges i .env.

### 4. Starta programmet

Öppna projektet i IntelliJ IDEA, låt Maven läsa in beroendena och kör klassen:

se.systementor.utbildning.Main

Programmet ansluter till PostgreSQL och MongoDB vid uppstart.

## Funktioner

### Pokedex

Användaren kan:

- Visa alla Pokemon.
- Söka efter Pokemon.
- Lägga till nya Pokemon.
- Redigera befintliga Pokemon.
- Ta bort Pokemon.
- Spara och läsa Pokemon-data via JSON.
- Återställa data från pokemon-seed.json.

Varje Pokemon har ett ID, namn, typ, HP, snabbhet och attacker.

Den huvudsakliga Pokemon-datahanteringen använder PostgreSQL genom JPA/Hibernate.

## Stridssystem

Spelaren väljer en Pokemon och möter en slumpmässigt vald motståndare.

Striderna är turbaserade och snabbheten avgör vem som attackerar först. Vid lika snabbhet avgör slumpen.

Spelaren väljer handling via en meny medan CPU använder en strategi för att välja attacker.

Attacker kan missa, orsaka varierande skada och ge kritiska träffar.

Striden avslutas när en Pokemon besegras eller spelaren flyr. Efteråt återställs deltagarnas HP.

## Skadeberäkning

Skadan beräknas enligt:

**Skada = max(1, heltal(grundskada × typeffektivitet × slumpfaktor))**

- **Basskada:** Attackens grundskada.
- **Typeffektivitet:** 2,0 vid typfördel, 0,5 vid typnackdel och annars 1,0 för de kombinationer programmet hanterar.
- **Slumpfaktor:** Mellan 0,85 och 1,00.
- **Kritisk träff:** 1/16 chans att fördubbla skadan.

En attack som missar orsakar 0 skada.

## Databashantering

Projektet använder två olika databastyper med separata ansvarsområden.

PostgreSQL och JPA/Hibernate

PostgreSQL är en relationsdatabas som används för att lagra Pokemon-data.

JPA/Hibernate hanterar kopplingen mellan Java-objekt och relationella tabeller.

Projektet använder bland annat:

- **PokemonDao** – interface för Pokemon-dataåtkomst.
- **JpaPokemonRepository** – implementation som använder JPA.
- **PokemonEntity** – databasrepresentation av Pokemon.
- **AttackEntity** – databasrepresentation av attacker.
- **DbConfig** – ansvarar för konfiguration av EntityManagerFactory.

Databasanslutningen konfigureras via .env.

## MongoDB

MongoDB används för att lagra stridsstatistik och individuella stridsloggar som BSON-dokument.

Projektet använder MongoDB Java Sync Driver direkt, utan Spring Data.

MongoDB-databasen innehåller två collections:

**battle_stats**

Lagrar sammanställd statistik för spelaren, bland annat:

Totalt antal vinster, förluster och flykter.

Vinster och förluster per Pokemon.

Hur ofta olika Pokemon används.

Hur ofta spelarens attacker används.

Statistiken lagras i ett dokument som uppdateras med replaceOne() och upsert(true).

**battleLogs**

Lagrar individuella strider som separata dokument.

Varje stridslogg innehåller:

- matchId – unik identifierare för striden.
- playedAt – tidpunkt för striden.
- participants – deltagande Pokemons ID.
- winnerPokemonId – vinnaren, eller null vid flykt.
- rounds – utförda attacker, rundnummer och faktisk HP-förlust.

## CRUD med MongoDB

Projektet implementerar CRUD-operationerna genom BattleLogRepository och MongoBattleLogRepository.


| Operation       | MongoDB-metod | Funktion                            |
|-----------------|---------------|-------------------------------------|
| `Create`        | insertOne()   | Skapar en stridslogg                |
| `Read`          | find()        | Hämtar stridsloggar                 |
| `Update`        | updateOne()   | Markerar en stridslogg som granskad |
| `Delete`        | deleteOne()   | Tar bort en stridslogg              |

Det finns även möjlighet att söka efter strider där en specifik Pokemon deltagit genom findByPokemonId().

CRUD-operationerna har verifierats genom manuella tester.

## Stridsstatistik

Programmet sparar statistik automatiskt efter varje avslutad strid.

Under **Spela** kan användaren välja mellan:

- Total statistik över vinster, förluster och flykter.
- Statistik för individuella Pokemon.
- Mest använda Pokemon.
- Mest använda attacker.

Vinstprocenten beräknas enligt:

**Vinster / (Vinster + Förluster) × 100**

Win/loss-ratio beräknas enligt:

**Vinster / Förluster**

Om en Pokemon har vinster men inga förluster visas ratio som oändlig (`∞`).

Statistiken kopplas till Pokemon-ID i stället för namn, så att historiken bevaras om en Pokemon byter namn.

Statistiken lagras nu i MongoDB i stället för i  `battle-stats.json`.

## Repository-mönstret

Projektet använder repository-mönstret för att separera applikationslogiken från databasåtkomsten.

**MongoBattleStatsRepository** implementerar interfacet för MongoDB.

**BattleService** använder interfacet och behöver därför inte känna till hur informationen lagras.

Samma princip används för Pokemon-data genom PokemonDao och för stridsloggar genom BattleLogRepository.

Det gör det möjligt att byta lagringsimplementation utan att behöva skriva om affärslogiken.

## Ansvarsfördelning

| Paket               | Ansvar                                                       |
|---------------------|--------------------------------------------------------------|
| `model`             | Pokemon, attacker, strider, statistik och stridsloggar       |
| `service`           | Programmets logik och stridsflöde                            |
| `repository`        | Interface för dataåtkomst och filhantering                   |
| `repository.jpa`    | PostgreSQL och JPA/Hibernate                                 |
| `repository.mongo`  | MongoDB och dokumenthantering                                |
| `config`            | Databaskonfiguration                                         |
| `ui`                | Menyer, presentation och användarinmatning                   |
| `exception`         | Egna undantagsklasser                                        |
| `strategy`          | CPU-strategier                                               |

## Objektorienterad programmering (OOP)
Projektet använder objektorienterad programmering för att separera ansvar och skapa en tydlig struktur.

### Inkapsling
Klassen `Pokemon` har privata attribut och ansvarar själv för att kontrollera exempelvis HP och antalet attacker.

`Battle` kapslar in stridens tillstånd genom metoder som `isOver()`, `playerWon()` och `flee()`.

### Arv och abstraktion
`Attack` är en abstrakt klass som innehåller gemensamma egenskaper för attacker.

`DamageAttack` ärver från `Attack` och implementerar hur en attack orsakar skada.

### Interfaces och polymorfism

**`BattleAction`** definierar gemensamma metoder för olika stridshandlingar.

Både attacker och flykt kan hanteras som BattleAction, trots att de har olika beteenden.

**`ActionResult`** används för representera resultat från olika handlingar, exempelvis `AttackResult` och `FleeResult`.

Repository-interfacen är ytterligare exempel på polymorfism eftersom olika implementationer kan användas genom samma interface.

### Enum
Type är en enum som representerar olika Pokemon-typer och förhindrar ogiltiga typnamn.

### Strategy Pattern

CPU använder `CpuStrategy` för att välja handlingar.

Två strategier finns:

- `RandomStrategy` väljer en slumpmässig handling. 
- `AggressiveStrategy` prioriterar skadeattacker.

Strategin skickas till  `CpuPlayer` via konstruktorn, vilket även är ett exempel på Dependency Injection.

## Filhantering och felhantering

Projektet innehåller fortfarande stöd för JSON-filhantering med Jackson.

- `pokemon-seed.json` – ursprunglig Pokemon-data.
- `pokemon.json` – fil för sparad Pokemon-data.

Filsökvägarna konfigureras via .env.

Stridsstatistiken lagras däremot i MongoDB och inte längre i `battle-stats.json`.

Programmet använder validering och egna undantagsklasser för att hantera ogiltig data och felaktig användarinmatning.

## Tester

Projektet innehåller JUnit-tester och har även testats manuellt.

MongoDB-operationerna Create, Read, Update och Delete har verifierats genom separata tester.

## Funktioner från tidigare kursmoment

Projektet innehåller funktioner som utvecklades under tidigare kursmoment:

- Typeffektivitet vid attacker.
- JSON-persistens med Jackson.
- Kritiska träffar.
- Persistent stridsstatistik.
- OOP med arv, interfaces och polymorfism.
- Strategy Pattern och Dependency Injection.

Dessa har senare kompletterats med PostgreSQL, JPA/Hibernate och MongoDB.

## Kända begränsningar

- Typeffektiviteten omfattar inte samtliga typkombinationer från de officiella Pokemon-spelen.
- Endast skadeattacker finns implementerade.
- MongoDB-lagringen använder den synkrona Java-drivrutinen.
- PostgreSQL och MongoDB används i samma applikation, men skrivningar till de två databaserna ingår inte i en gemensam transaktion.
- Den lokala MongoDB-konfigurationen är avsedd för utveckling och använder för närvarande ingen autentisering.