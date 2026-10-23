# Java-tekster til vurdering af flersprogethed

Dato: 22. oktober 2026. Statisk gennemgang af alle 620 Java-filer under `src/main/java` i de fire moduler. Testkode, kommentarer, eksterne biblioteker og genererede build-filer er ikke med.

## Afgrænsning og tælling

`getFieldLabelName()`, `getFieldHeaderName()` og hele `PageType.java` er udeladt fra tekstlisten. Feltnavne/enum-beskrivelser andre steder er fortsat med, når de er tekstkandidater. SQL, direkte logkald, XML/JSON-nøgler og andre tekniske strenge er sorteret fra. Fejltekster gemt inde i SQL er særskilt markeret.

**2157 forekomster, 1727 forskellige Java-strenge, fordelt på 208 filer.** En forekomst er én strengliteral; en sammensat besked kan derfor fylde flere rækker. Ens tekster tælles kun én gang i antallet af forskellige strenge. Tomme og rent numeriske strenge er ikke tekstkandidater. Java-escapes er bevaret.

Listen er et komplet udtræk efter de beskrevne statiske udvælgelsesregler, ikke en verificeret liste over tekster, der faktisk vises ved kørsel. Særligt exceptions, statuskoder, historik og eksempeldata kræver en beslutning om oversættelsesomfang. Et præcist antal oversættelsesnøgler kræver også samling af fragmenter til hele beskeder.

For at gøre frasorteringen kontrollerbar findes **alle 13838 strengforekomster** i [alle-strenge.csv](alle-strenge.csv), inklusive kategori og kildekodelinje. Det sikrer, at også strenge, som udvælgelsesreglerne har frasorteret, kan gennemgås. [tekstliste.csv](tekstliste.csv) indeholder selve tekstlisten med semikolon som separator og UTF-8 BOM til Excel. Tekster hentet fra database, ressourcer eller eksterne APIer er ikke strengliteraler i Java og indgår ikke.

## Fordeling

| Modul | Forekomster | Forskellige strenge | Filer |
|---|---:|---:|---:|
| web-server | 947 | 678 | 137 |
| common | 1096 | 1005 | 60 |
| integration-server | 41 | 36 | 3 |
| timesheet-app | 73 | 59 | 8 |

Antallet af forskellige strenge pr. modul kan ikke summeres, fordi samme tekst kan forekomme i flere moduler.

| Kategori | Forekomster | Forskellige strenge |
|---|---:|---:|
| Øvrige tekstkandidater | 514 | 417 |
| Fejl og exceptions | 534 | 403 |
| Korte tekstkandidater | 95 | 38 |
| Svar og validering | 13 | 13 |
| Indlejret HTML/XML | 2 | 2 |
| Eksport og import | 79 | 43 |
| Enum-tekster | 901 | 820 |
| SQL med gemt fejltekst | 3 | 3 |
| Skærmtekster og metadata | 16 | 16 |

## Tekstliste pr. fil

### common/src/main/java/com/bepa/eis/common/enums/EventType.java

3 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [5](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/EventType.java:5) | Enum-tekster | Create Customer |
| [6](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/EventType.java:6) | Enum-tekster | Entity Created or Modified |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/EventType.java:7) | Enum-tekster | Invalid Event Type |

### common/src/main/java/com/bepa/eis/common/enums/ServiceType.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [5](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/ServiceType.java:5) | Enum-tekster | Web service |
| [6](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/ServiceType.java:6) | Enum-tekster | Provider Service |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/ServiceType.java:7) | Enum-tekster | Entity processing |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/ServiceType.java:8) | Enum-tekster | Invalid Service Type |

### common/src/main/java/com/bepa/eis/common/enums/SeverityType.java

5 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [5](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/SeverityType.java:5) | Enum-tekster | Critical |
| [6](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/SeverityType.java:6) | Enum-tekster | High |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/SeverityType.java:7) | Enum-tekster | Medium |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/SeverityType.java:8) | Enum-tekster | Low |
| [9](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/SeverityType.java:9) | Enum-tekster | Severity Type |

### common/src/main/java/com/bepa/eis/common/enums/customer/BillingPeriod.java

8 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/BillingPeriod.java:7) | Enum-tekster | Monthly |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/BillingPeriod.java:7) | Enum-tekster | Monthly billing period |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/BillingPeriod.java:8) | Enum-tekster | Quarterly |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/BillingPeriod.java:8) | Enum-tekster | Quarterly billing period |
| [9](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/BillingPeriod.java:9) | Enum-tekster | Semi-annual |
| [9](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/BillingPeriod.java:9) | Enum-tekster | Semi-annual billing period |
| [10](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/BillingPeriod.java:10) | Enum-tekster | Annually |
| [10](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/BillingPeriod.java:10) | Enum-tekster | Annual billing period |

### common/src/main/java/com/bepa/eis/common/enums/customer/CustomerModuleStatus.java

10 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerModuleStatus.java:7) | Enum-tekster | Active |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerModuleStatus.java:8) | Enum-tekster | Customer module is active. |
| [13](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerModuleStatus.java:13) | Enum-tekster | Trial |
| [14](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerModuleStatus.java:14) | Enum-tekster | Customer module is active during the trial period. |
| [19](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerModuleStatus.java:19) | Enum-tekster | Suspended |
| [20](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerModuleStatus.java:20) | Enum-tekster | Customer module has been suspended. |
| [25](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerModuleStatus.java:25) | Enum-tekster | Cancelled |
| [26](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerModuleStatus.java:26) | Enum-tekster | Customer module has been cancelled. |
| [31](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerModuleStatus.java:31) | Enum-tekster | Expired |
| [32](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerModuleStatus.java:32) | Enum-tekster | Customer module has expired. |

### common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentMethodStatus.java

8 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentMethodStatus.java:7) | Enum-tekster | Active |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentMethodStatus.java:8) | Enum-tekster | Payment method is active and can be used for payments. |
| [13](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentMethodStatus.java:13) | Enum-tekster | Expired |
| [14](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentMethodStatus.java:14) | Enum-tekster | Payment method has expired and should not be used. |
| [19](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentMethodStatus.java:19) | Enum-tekster | Disabled |
| [20](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentMethodStatus.java:20) | Enum-tekster | Payment method has been disabled and should not be used. |
| [25](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentMethodStatus.java:25) | Enum-tekster | Deleted |
| [26](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentMethodStatus.java:26) | Enum-tekster | Payment method has been deleted or removed from active use. |

### common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java

34 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [6](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:6) | Enum-tekster | None |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:7) | Enum-tekster | No payment has been created. |
| [11](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:11) | Enum-tekster | Created |
| [12](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:12) | Enum-tekster | Payment record has been created, but payment processing has not yet started. |
| [16](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:16) | Enum-tekster | Requested |
| [17](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:17) | Enum-tekster | Payment has been requested from the payment provider. |
| [21](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:21) | Enum-tekster | Pending |
| [22](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:22) | Enum-tekster | Payment is pending and awaiting confirmation from the payment provider. |
| [26](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:26) | Enum-tekster | Authorized |
| [27](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:27) | Enum-tekster | Payment has been authorized but not yet captured. |
| [31](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:31) | Enum-tekster | Captured |
| [32](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:32) | Enum-tekster | Payment has been captured successfully. |
| [36](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:36) | Enum-tekster | Succeeded |
| [37](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:37) | Enum-tekster | Payment has been completed successfully. |
| [41](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:41) | Enum-tekster | Failed |
| [42](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:42) | Enum-tekster | Payment has failed. |
| [46](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:46) | Enum-tekster | Cancelled |
| [47](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:47) | Enum-tekster | Payment has been cancelled. |
| [51](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:51) | Enum-tekster | Rejected |
| [52](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:52) | Enum-tekster | Payment has been rejected by the payment provider or issuer. |
| [56](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:56) | Enum-tekster | Expired |
| [57](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:57) | Enum-tekster | Payment request has expired before completion. |
| [61](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:61) | Enum-tekster | Timed out |
| [62](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:62) | Enum-tekster | Payment did not complete before the configured timeout. |
| [66](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:66) | Enum-tekster | Refunded |
| [67](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:67) | Enum-tekster | Payment has been refunded. |
| [71](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:71) | Enum-tekster | Partially refunded |
| [72](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:72) | Enum-tekster | Payment has been partially refunded. |
| [76](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:76) | Enum-tekster | Disputed |
| [77](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:77) | Enum-tekster | Payment has been disputed. |
| [81](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:81) | Enum-tekster | Overdue |
| [82](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:82) | Enum-tekster | Payment has not been received before the due date. |
| [86](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:86) | Enum-tekster | Manual review |
| [87](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerPaymentStatus.java:87) | Enum-tekster | Payment requires manual review. |

### common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java

20 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:7) | Enum-tekster | Created |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:8) | Enum-tekster | Customer has been created, but onboarding has not yet started. |
| [13](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:13) | Enum-tekster | Pending confirmation |
| [14](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:14) | Enum-tekster | Customer must confirm account creation before access is enabled. |
| [19](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:19) | Enum-tekster | Trial active |
| [20](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:20) | Enum-tekster | Customer is in the trial period and access is allowed. |
| [25](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:25) | Enum-tekster | Pending subscription confirmation |
| [26](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:26) | Enum-tekster | Customer must confirm continuation of the subscription. |
| [31](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:31) | Enum-tekster | Payment pending |
| [32](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:32) | Enum-tekster | Payment has been requested and the system is waiting for payment confirmation. |
| [37](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:37) | Enum-tekster | Subscription active |
| [38](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:38) | Enum-tekster | Customer has an active subscription and access is allowed. |
| [43](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:43) | Enum-tekster | Subscription expiring |
| [44](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:44) | Enum-tekster | Customer subscription is close to expiry. |
| [49](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:49) | Enum-tekster | Payment overdue |
| [50](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:50) | Enum-tekster | Payment is overdue, but customer may still be inside the grace period. |
| [55](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:55) | Enum-tekster | Suspended |
| [56](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:56) | Enum-tekster | Customer is suspended and access is not allowed. |
| [61](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:61) | Enum-tekster | Cancelled |
| [62](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerStatus.java:62) | Enum-tekster | Customer has been cancelled. |

### common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java

26 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [6](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:6) | Enum-tekster | None |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:7) | Enum-tekster | Customer has no active trial or subscription. |
| [11](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:11) | Enum-tekster | Trial |
| [12](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:12) | Enum-tekster | Customer is currently in the trial period. |
| [16](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:16) | Enum-tekster | Trial expiring |
| [17](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:17) | Enum-tekster | Customer trial period is close to expiry. |
| [21](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:21) | Enum-tekster | Trial expired |
| [22](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:22) | Enum-tekster | Customer trial period has expired. |
| [26](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:26) | Enum-tekster | Pending confirmation |
| [27](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:27) | Enum-tekster | Customer must confirm continuation or renewal of the subscription. |
| [31](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:31) | Enum-tekster | Payment pending |
| [32](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:32) | Enum-tekster | Subscription payment has been requested and is waiting for confirmation. |
| [36](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:36) | Enum-tekster | Active |
| [37](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:37) | Enum-tekster | Customer subscription is active. |
| [41](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:41) | Enum-tekster | Expiring |
| [42](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:42) | Enum-tekster | Customer subscription is close to expiry. |
| [46](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:46) | Enum-tekster | Expired |
| [47](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:47) | Enum-tekster | Customer subscription period has expired. |
| [51](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:51) | Enum-tekster | Payment overdue |
| [52](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:52) | Enum-tekster | Payment has not been received before the due date. |
| [56](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:56) | Enum-tekster | Grace period |
| [57](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:57) | Enum-tekster | Customer subscription has expired, but the payment grace period is still active. |
| [61](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:61) | Enum-tekster | Suspended |
| [62](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:62) | Enum-tekster | Customer subscription is suspended. |
| [66](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:66) | Enum-tekster | Cancelled |
| [67](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerSubscriptionStatus.java:67) | Enum-tekster | Customer subscription has been cancelled. |

### common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java

14 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [6](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:6) | Enum-tekster | Email confirmation |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:7) | Enum-tekster | Token used when the customer must confirm account creation. |
| [11](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:11) | Enum-tekster | Subscription continuation |
| [12](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:12) | Enum-tekster | Token used when the customer must confirm continuation after the trial period. |
| [16](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:16) | Enum-tekster | Subscription renewal |
| [17](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:17) | Enum-tekster | Token used when the customer must confirm subscription renewal. |
| [21](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:21) | Enum-tekster | Payment update |
| [22](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:22) | Enum-tekster | Token used when the customer must update or retry payment. |
| [26](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:26) | Enum-tekster | Payment confirmation |
| [27](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:27) | Enum-tekster | Token used when the customer must confirm or complete payment. |
| [31](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:31) | Enum-tekster | Reactivation |
| [32](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:32) | Enum-tekster | Token used when the customer must reactivate a suspended account. |
| [36](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:36) | Enum-tekster | Admin reactivation |
| [37](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerTokenType.java:37) | Enum-tekster | Token used for administrator-initiated reactivation flows. |

### common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java

70 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [6](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:6) | Enum-tekster | Customer created |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:7) | Enum-tekster | A customer has been created and the customer onboarding workflow should start. |
| [12](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:12) | Enum-tekster | Customer updated |
| [13](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:13) | Enum-tekster | Customer master data has been updated. |
| [18](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:18) | Enum-tekster | Customer payment info updated |
| [19](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:19) | Enum-tekster | Customer payment information has been updated. |
| [24](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:24) | Enum-tekster | Customer email confirmation requested |
| [25](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:25) | Enum-tekster | A customer email confirmation has been requested. |
| [30](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:30) | Enum-tekster | Customer email confirmation sent |
| [31](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:31) | Enum-tekster | Customer email confirmation mail has been queued or sent. |
| [36](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:36) | Enum-tekster | Customer email confirmed |
| [37](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:37) | Enum-tekster | Customer has confirmed the account email address. |
| [42](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:42) | Enum-tekster | Customer email confirmation expired |
| [43](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:43) | Enum-tekster | Customer did not confirm the account email address before the confirmation deadline. |
| [48](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:48) | Enum-tekster | Trial started |
| [49](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:49) | Enum-tekster | Customer trial period has started. |
| [54](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:54) | Enum-tekster | Trial expiring soon |
| [55](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:55) | Enum-tekster | Customer trial period is close to expiry and a reminder should be sent. |
| [60](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:60) | Enum-tekster | Trial expiration mail sent |
| [61](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:61) | Enum-tekster | Trial expiration reminder mail has been queued or sent. |
| [66](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:66) | Enum-tekster | Trial expired |
| [67](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:67) | Enum-tekster | Customer trial period has expired. |
| [72](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:72) | Enum-tekster | Subscription continuation confirmed |
| [73](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:73) | Enum-tekster | Customer has confirmed that the subscription should continue after the trial period. |
| [78](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:78) | Enum-tekster | Subscription continuation confirmation expired |
| [79](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:79) | Enum-tekster | Customer did not confirm continuation of the subscription before the trial ended. |
| [84](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:84) | Enum-tekster | Payment requested |
| [85](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:85) | Enum-tekster | A payment request has been created. |
| [90](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:90) | Enum-tekster | Payment succeeded |
| [91](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:91) | Enum-tekster | Payment has been completed successfully. |
| [96](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:96) | Enum-tekster | Payment failed |
| [97](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:97) | Enum-tekster | Payment failed or was rejected. |
| [102](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:102) | Enum-tekster | Payment cancelled |
| [103](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:103) | Enum-tekster | Payment was cancelled. |
| [108](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:108) | Enum-tekster | Payment timed out |
| [109](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:109) | Enum-tekster | Payment was not completed before the payment timeout. |
| [114](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:114) | Enum-tekster | Payment overdue |
| [115](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:115) | Enum-tekster | Payment has not been received before the due date. |
| [120](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:120) | Enum-tekster | Payment grace period expired |
| [121](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:121) | Enum-tekster | Payment has not been received before the grace period expired. |
| [126](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:126) | Enum-tekster | Subscription activated |
| [127](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:127) | Enum-tekster | Customer subscription has been activated. |
| [132](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:132) | Enum-tekster | Subscription expiring soon |
| [133](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:133) | Enum-tekster | Customer subscription is close to expiry and a renewal reminder should be sent. |
| [138](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:138) | Enum-tekster | Subscription expiration mail sent |
| [139](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:139) | Enum-tekster | Subscription expiration reminder mail has been queued or sent. |
| [144](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:144) | Enum-tekster | Subscription expired |
| [145](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:145) | Enum-tekster | Customer subscription has expired. |
| [150](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:150) | Enum-tekster | Subscription renewal confirmed |
| [151](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:151) | Enum-tekster | Customer has confirmed renewal of the subscription. |
| [156](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:156) | Enum-tekster | Customer suspended |
| [157](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:157) | Enum-tekster | Customer has been suspended by the workflow. |
| [162](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:162) | Enum-tekster | Customer suspension mail sent |
| [163](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:163) | Enum-tekster | Customer suspension mail has been queued or sent. |
| [168](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:168) | Enum-tekster | Customer reactivated |
| [169](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:169) | Enum-tekster | Customer has been reactivated. |
| [174](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:174) | Enum-tekster | Customer cancelled |
| [175](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:175) | Enum-tekster | Customer has been cancelled. |
| [180](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:180) | Enum-tekster | Customer manually suspended |
| [181](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:181) | Enum-tekster | Customer has been manually suspended by an administrator. |
| [186](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:186) | Enum-tekster | Customer manually reactivated |
| [187](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:187) | Enum-tekster | Customer has been manually reactivated by an administrator. |
| [192](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:192) | Enum-tekster | Customer manually cancelled |
| [193](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:193) | Enum-tekster | Customer has been manually cancelled by an administrator. |
| [198](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:198) | Enum-tekster | Workflow retry requested |
| [199](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:199) | Enum-tekster | A retry has been requested for the customer workflow. |
| [204](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:204) | Enum-tekster | Workflow error |
| [205](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:205) | Enum-tekster | An error occurred while processing the customer workflow. |
| [210](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:210) | Enum-tekster | Workflow manual attention required |
| [211](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowEventType.java:211) | Enum-tekster | The customer workflow requires manual attention. |

### common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java

20 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:8) | Enum-tekster | Customer confirmation |
| [9](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:9) | Enum-tekster | Mail asking the customer to confirm account creation. |
| [15](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:15) | Enum-tekster | Trial expiring |
| [16](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:16) | Enum-tekster | Mail informing the customer that the trial period is close to expiry. |
| [22](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:22) | Enum-tekster | Subscription continuation confirmation |
| [23](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:23) | Enum-tekster | Mail asking the customer to confirm continuation of the subscription after the trial period. |
| [29](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:29) | Enum-tekster | Payment requested |
| [30](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:30) | Enum-tekster | Mail informing the customer that payment has been requested. |
| [36](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:36) | Enum-tekster | Payment failed |
| [37](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:37) | Enum-tekster | Mail informing the customer that payment failed or could not be completed. |
| [43](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:43) | Enum-tekster | Payment overdue |
| [44](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:44) | Enum-tekster | Mail informing the customer that payment is overdue. |
| [50](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:50) | Enum-tekster | Subscription expiring |
| [51](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:51) | Enum-tekster | Mail informing the customer that the subscription is close to expiry. |
| [57](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:57) | Enum-tekster | Subscription renewal confirmation |
| [58](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:58) | Enum-tekster | Mail asking the customer to confirm renewal of the subscription. |
| [64](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:64) | Enum-tekster | Customer suspended |
| [65](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:65) | Enum-tekster | Mail informing the customer that the account has been suspended. |
| [71](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:71) | Enum-tekster | Customer reactivated |
| [72](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowMailType.java:72) | Enum-tekster | Mail informing the customer that the account has been reactivated. |

### common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java

26 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [6](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:6) | Enum-tekster | Created |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:7) | Enum-tekster | Customer has been created, but the workflow has not yet started. |
| [11](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:11) | Enum-tekster | Pending email confirmation |
| [12](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:12) | Enum-tekster | Customer confirmation email has been sent and the workflow is waiting for the customer to confirm the account. |
| [16](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:16) | Enum-tekster | Email confirmed |
| [17](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:17) | Enum-tekster | Customer has confirmed the account email address. |
| [21](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:21) | Enum-tekster | Trial active |
| [22](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:22) | Enum-tekster | Customer trial period is active and users are allowed to log in. |
| [26](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:26) | Enum-tekster | Trial expiring |
| [27](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:27) | Enum-tekster | Customer trial period is close to expiry and a continuation confirmation is required. |
| [31](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:31) | Enum-tekster | Pending subscription confirmation |
| [32](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:32) | Enum-tekster | The workflow is waiting for the customer to confirm continuation of the subscription. |
| [36](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:36) | Enum-tekster | Payment pending |
| [37](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:37) | Enum-tekster | A payment request has been created and the workflow is waiting for payment confirmation. |
| [41](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:41) | Enum-tekster | Subscription active |
| [42](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:42) | Enum-tekster | Customer subscription is active and users are allowed to log in. |
| [46](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:46) | Enum-tekster | Subscription expiring |
| [47](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:47) | Enum-tekster | Customer subscription is close to expiry and renewal confirmation is required. |
| [51](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:51) | Enum-tekster | Payment overdue |
| [52](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:52) | Enum-tekster | Payment has not been received before the due date, but the customer may still be inside the grace period. |
| [56](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:56) | Enum-tekster | Suspended |
| [57](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:57) | Enum-tekster | Customer has been suspended and users are not allowed to log in. |
| [61](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:61) | Enum-tekster | Cancelled |
| [62](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:62) | Enum-tekster | Customer workflow has been cancelled. |
| [66](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:66) | Enum-tekster | Waiting for manual attention |
| [67](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowState.java:67) | Enum-tekster | Customer workflow cannot continue automatically and requires manual handling. |

### common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowStatus.java

10 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [6](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowStatus.java:6) | Enum-tekster | Active |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowStatus.java:7) | Enum-tekster | The customer workflow is active and can be processed automatically. |
| [11](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowStatus.java:11) | Enum-tekster | Completed |
| [12](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowStatus.java:12) | Enum-tekster | The customer workflow has completed successfully. |
| [16](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowStatus.java:16) | Enum-tekster | Cancelled |
| [17](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowStatus.java:17) | Enum-tekster | The customer workflow has been cancelled. |
| [21](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowStatus.java:21) | Enum-tekster | Suspended |
| [22](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowStatus.java:22) | Enum-tekster | The customer workflow is suspended and will not continue automatically until reactivated. |
| [26](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowStatus.java:26) | Enum-tekster | Waiting for manual attention |
| [27](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/CustomerWorkflowStatus.java:27) | Enum-tekster | The customer workflow requires manual attention before it can continue. |

### common/src/main/java/com/bepa/eis/common/enums/customer/Subscription.java

3 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/Subscription.java:7) | Enum-tekster | Basis Module |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/Subscription.java:8) | Enum-tekster | Pro Module |
| [9](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/customer/Subscription.java:9) | Enum-tekster | Master Module |

### common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java

66 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:8) | Enum-tekster | SBS code |
| [9](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:9) | Enum-tekster | Code level |
| [10](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:10) | Enum-tekster | System Name |
| [11](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:11) | Enum-tekster | System Owner |
| [12](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:12) | Enum-tekster | Department |
| [15](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:15) | Enum-tekster | Trl |
| [16](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:16) | Enum-tekster | Deadline Next TRL |
| [17](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:17) | Enum-tekster | Deadline Finalized |
| [19](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:19) | Enum-tekster | Project Name |
| [23](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:23) | Enum-tekster | Name |
| [24](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:24) | Enum-tekster | Description |
| [25](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:25) | Enum-tekster | High Level Capability |
| [26](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:26) | Enum-tekster | Type |
| [27](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:27) | Enum-tekster | Frequency |
| [28](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:28) | Enum-tekster | Performance |
| [29](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:29) | Enum-tekster | Verification Statement |
| [30](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:30) | Enum-tekster | Verification Status |
| [31](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:31) | Enum-tekster | Rationale Statement |
| [32](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:32) | Enum-tekster | Business Priority |
| [33](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:33) | Enum-tekster | Technical Priority |
| [34](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:34) | Enum-tekster | Capture Date |
| [35](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:35) | Enum-tekster | Requirement Owner |
| [36](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:36) | Enum-tekster | Requirement Status |
| [38](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:38) | Enum-tekster | Supplier |
| [39](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:39) | Enum-tekster | Contractor |
| [40](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:40) | Enum-tekster | Stakeholder Name |
| [41](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:41) | Enum-tekster | Stakeholder Description |
| [42](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:42) | Enum-tekster | Stakeholder Contact |
| [43](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:43) | Enum-tekster | Stakeholder Contact Email |
| [44](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:44) | Enum-tekster | Stakeholder Contact Phone |
| [45](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:45) | Enum-tekster | Stakeholder |
| [47](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:47) | Enum-tekster | Stakeholder Requirement Code |
| [48](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:48) | Enum-tekster | Systems Requirement Code |
| [50](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:50) | Enum-tekster | Logical Code |
| [51](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:51) | Enum-tekster | Logical Name |
| [52](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:52) | Enum-tekster | Logical Description |
| [54](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:54) | Enum-tekster | Functional Code |
| [55](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:55) | Enum-tekster | Functional Name |
| [56](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:56) | Enum-tekster | Functional Description |
| [57](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:57) | Enum-tekster | Function Owner |
| [58](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:58) | Enum-tekster | Function Status |
| [59](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:59) | Enum-tekster | Function Behavior Type |
| [60](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:60) | Enum-tekster | Function Category |
| [61](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:61) | Enum-tekster | Function Criticality |
| [62](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:62) | Enum-tekster | Function Verification Status |
| [63](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:63) | Enum-tekster | Function Level |
| [64](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:64) | Enum-tekster | Function Operating Mode |
| [65](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:65) | Enum-tekster | Function Responsible Domain |
| [66](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:66) | Enum-tekster | Function Configuration Variant |
| [67](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:67) | Enum-tekster | Function Applicability |
| [70](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:70) | Enum-tekster | Logical Owner |
| [71](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:71) | Enum-tekster | Logical Verification Status |
| [72](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:72) | Enum-tekster | Logical Criticality |
| [73](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:73) | Enum-tekster | Logical Element Category |
| [74](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:74) | Enum-tekster | Logical Level |
| [75](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:75) | Enum-tekster | Logical Type |
| [76](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:76) | Enum-tekster | Logical Responsible Domain |
| [77](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:77) | Enum-tekster | Logical Lifecycle Status |
| [78](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:78) | Enum-tekster | Logical Maturity |
| [79](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:79) | Enum-tekster | Logical Allocation Status |
| [80](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:80) | Enum-tekster | Logical Realization Status |
| [81](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:81) | Enum-tekster | Logical Safety Classification |
| [82](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:82) | Enum-tekster | Logical Security Classification |
| [83](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:83) | Enum-tekster | Logical Redundancy Type |
| [84](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:84) | Enum-tekster | Logical Configuration Variant |
| [85](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityDataElement.java:85) | Enum-tekster | Logical Applicability |

### common/src/main/java/com/bepa/eis/common/enums/entity/EntityType.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [20](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityType.java:20) | Øvrige tekstkandidater | Physical Architecture |
| [50](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityType.java:50) | Øvrige tekstkandidater | Stakeholder requirement |
| [64](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityType.java:64) | Øvrige tekstkandidater | System requirement |
| [93](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityType.java:93) | Øvrige tekstkandidater | Logical Architecture |
| [120](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityType.java:120) | Øvrige tekstkandidater | Functional Architecture |
| [217](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/EntityType.java:217) | Fejl og exceptions | Unknown EntityType id:  |

### common/src/main/java/com/bepa/eis/common/enums/entity/RelationType.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [5](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/RelationType.java:5) | Enum-tekster | Deleted |
| [6](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/RelationType.java:6) | Enum-tekster | Confirmed |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/RelationType.java:7) | Enum-tekster | Not Relevant |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/RelationType.java:8) | Enum-tekster | Relation Type |

### common/src/main/java/com/bepa/eis/common/enums/entity/SBSCodeTypes.java

5 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [4](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/SBSCodeTypes.java:4) | Enum-tekster | Function |
| [5](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/SBSCodeTypes.java:5) | Enum-tekster | Location |
| [6](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/SBSCodeTypes.java:6) | Enum-tekster | Product |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/SBSCodeTypes.java:7) | Enum-tekster | Type or classification |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/entity/SBSCodeTypes.java:8) | Enum-tekster | Other |

### common/src/main/java/com/bepa/eis/common/enums/mail/MailStatus.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [5](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailStatus.java:5) | Enum-tekster | Queued |
| [6](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailStatus.java:6) | Enum-tekster | Sending |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailStatus.java:7) | Enum-tekster | Sent |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailStatus.java:8) | Enum-tekster | Failed |
| [9](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailStatus.java:9) | Enum-tekster | Undelivered |
| [10](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailStatus.java:10) | Enum-tekster | Cancelled |

### common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java

17 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [5](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:5) | Enum-tekster | Password Reset |
| [6](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:6) | Enum-tekster | MFA Reset |
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:7) | Enum-tekster | User Created |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:8) | Enum-tekster | User Invited |
| [9](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:9) | Enum-tekster | Requirement Assigned |
| [10](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:10) | Enum-tekster | Requirement Changed |
| [11](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:11) | Enum-tekster | System Notification |
| [13](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:13) | Enum-tekster | Customer Confirmation |
| [14](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:14) | Enum-tekster | Trial Expiring |
| [15](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:15) | Enum-tekster | Subscription Continuation Confirmation |
| [16](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:16) | Enum-tekster | Payment Requested |
| [17](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:17) | Enum-tekster | Payment Failed |
| [18](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:18) | Enum-tekster | Payment Overdue |
| [19](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:19) | Enum-tekster | Subscription Expiring |
| [20](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:20) | Enum-tekster | Subscription Renewal Confirmation |
| [21](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:21) | Enum-tekster | Customer Suspended |
| [22](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/mail/MailTemplateType.java:22) | Enum-tekster | Customer Reactivated |

### common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java

19 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:7) | Enum-tekster | Home |
| [13](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:13) | Enum-tekster | Folder |
| [19](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:19) | Enum-tekster | Settings |
| [25](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:25) | Enum-tekster | Dashboard |
| [31](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:31) | Enum-tekster | Chart |
| [37](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:37) | Enum-tekster | Requirement |
| [43](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:43) | Enum-tekster | Functional |
| [49](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:49) | Enum-tekster | Logical |
| [55](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:55) | Enum-tekster | Physical |
| [61](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:61) | Enum-tekster | Link |
| [67](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:67) | Enum-tekster | List |
| [73](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:73) | Enum-tekster | Grid |
| [79](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:79) | Enum-tekster | Help |
| [85](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:85) | Enum-tekster | Overview |
| [91](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:91) | Enum-tekster | PDF |
| [97](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:97) | Enum-tekster | Relation |
| [103](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:103) | Enum-tekster | Search |
| [109](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:109) | Enum-tekster | Settings |
| [115](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuIcon.java:115) | Enum-tekster | N2 |

### common/src/main/java/com/bepa/eis/common/enums/menu/MenuItemType.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuItemType.java:7) | Enum-tekster | Header |
| [13](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/menu/MenuItemType.java:13) | Enum-tekster | Menu item |

### common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java

437 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:8) | Enum-tekster | Cover all A codes - Software Control |
| [9](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:9) | Enum-tekster | Software controlled interfaces |
| [10](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:10) | Enum-tekster | UI, GUI, API, USB, REST, TCP |
| [15](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:15) | Enum-tekster | User Interfaces (UI) |
| [16](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:16) | Enum-tekster | GUI, Command-Line (CLI), menu-driven, touch, voice, gestures |
| [22](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:22) | Enum-tekster | Software-to-Software interfaces |
| [23](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:23) | Enum-tekster | APIs, SDK, Libraries, Function/procedure calls |
| [29](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:29) | Enum-tekster | System Interfaces |
| [30](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:30) | Enum-tekster | Interfaces between software and operation systems (eg. Backbones), databases, file systems, or external systems |
| [36](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:36) | Enum-tekster | Hardware Interfaces |
| [37](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:37) | Enum-tekster | Software interaction with sensors, actuators, controllers, USB/serial devices etc. |
| [43](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:43) | Enum-tekster | Communication Interfaces |
| [44](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:44) | Enum-tekster | REST, SOAP, WebSockets, TCP/IP, messaging queues, RPC |
| [50](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:50) | Enum-tekster | Data Interfaces |
| [51](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:51) | Enum-tekster | Files, databases, shared memory, datastreams, import/export formats |
| [57](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:57) | Enum-tekster | Control Interfaces |
| [58](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:58) | Enum-tekster | Interfaces used to issue commands, change operating modes, configure parameters, or start/stop processes |
| [64](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:64) | Enum-tekster | Monitoring/Diagnostic Interfaces |
| [65](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:65) | Enum-tekster | Interfaces providing status, logs, alarms, health information, and diagnostic data |
| [71](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:71) | Enum-tekster | Security Interfaces |
| [72](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:72) | Enum-tekster | Authentication, authorization, encryption/key-management, and access-control interfaces |
| [78](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:78) | Enum-tekster | Cover all B codes - Converting an input variable (physical property, condition or event) into a signal for further processing |
| [79](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:79) | Enum-tekster | Detecting\nSensing\nRecording Measurements\nMonitoring |
| [80](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:80) | Enum-tekster | Auxiliary switch (as position indicator)\nBuchholz relay\nCurrent transformer\nDifferential protection relays\nDistance protection relays\nFlame detector\nGas detector\nLimit switch\nMeasuring element\nMeasuring relay\nMeasuring shunt (resistance)\nMicrophone\nDetector\nMovement detector\nOptical current transformer\nOptical voltage transformer\nOver-current protection relay\nOverload relay\nPhotocell\nPosition switch\nProtection relay\nProtection cubicle\nProximity sensor\nProximity switch\nSafety cabinet\nSensor\nSmoke sensor\nTemperature sensor\nVideo camera\nVoltage transformer |
| [85](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:85) | Enum-tekster | Electrical potential |
| [86](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:86) | Enum-tekster | Measuring relay (voltage), measuring shunt (voltage), measuring transformer (voltage), protection relay (voltage), voltage transformer |
| [92](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:92) | Enum-tekster | Electrical current |
| [93](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:93) | Enum-tekster | Current transformer, measuring relay (current), measuring transformer (current), protection relay (current), overload relay (current) (Shunt) |
| [99](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:99) | Enum-tekster | Density |
| [106](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:106) | Enum-tekster | Other electrical or electromagnetic variable |
| [107](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:107) | Enum-tekster | Measuring relay, measuring shunt (resistance), measuring transformer, protection relay |
| [113](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:113) | Enum-tekster | Flow |
| [114](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:114) | Enum-tekster | Flow meter, gas meter, water meter |
| [120](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:120) | Enum-tekster | Gauge, position, length (including distance, elongation, amplitude) |
| [121](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:121) | Enum-tekster | Motion sensor, movement detector, position switch, proximity switch, proximity sensor |
| [127](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:127) | Enum-tekster | Power |
| [134](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:134) | Enum-tekster | Time |
| [135](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:135) | Enum-tekster | Clock, time counter |
| [141](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:141) | Enum-tekster | Level |
| [142](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:142) | Enum-tekster | Level sensor |
| [148](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:148) | Enum-tekster | Moisture, Humidity |
| [149](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:149) | Enum-tekster | Humidity meter |
| [155](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:155) | Enum-tekster | Pressure, Vacuum |
| [156](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:156) | Enum-tekster | Pressure gauge, pressure sensor |
| [162](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:162) | Enum-tekster | Quality (composition, concentration, purity, material property) |
| [163](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:163) | Enum-tekster | Gas analyzer, x-ray machine |
| [169](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:169) | Enum-tekster | Radiation |
| [170](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:170) | Enum-tekster | Flame detector, smoke detector |
| [176](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:176) | Enum-tekster | Speed, frequency (including acceleration) |
| [177](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:177) | Enum-tekster | Accelerometer, speedometer, tachometer, vibration pickup |
| [183](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:183) | Enum-tekster | Temperature |
| [184](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:184) | Enum-tekster | Temperature sensor, temperature monitor |
| [190](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:190) | Enum-tekster | Multi-variable |
| [191](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:191) | Enum-tekster | Buchholz relay, protection cubicle |
| [197](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:197) | Enum-tekster | Weight, Force |
| [198](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:198) | Enum-tekster | Load cell |
| [204](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:204) | Enum-tekster | Other quantities |
| [205](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:205) | Enum-tekster | Microphone, video camera |
| [211](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:211) | Enum-tekster | Number of events, counts, combined tasks |
| [212](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:212) | Enum-tekster | Switching cycle detector, combined instrument transformer |
| [218](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:218) | Enum-tekster | Cover all C codes - Storing of energy, information or material |
| [219](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:219) | Enum-tekster | Recording  storage |
| [220](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:220) | Enum-tekster | Buffer battery\nCapacitor\nEvent recorder (mainly for storage\npurposes)\nHard disk\nMagnetic tape recorder (mainly for storage purposes)\nMemory\nRAM\nStorage battery\nVideo recorder (mainly for storage purposes)\nVoltage recorder (mainly for storage purposes) Water tank |
| [225](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:225) | Enum-tekster | Capacitive storage of electric energy |
| [226](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:226) | Enum-tekster | Capacitor |
| [232](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:232) | Enum-tekster | Inductive storage of electric energy |
| [233](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:233) | Enum-tekster | Coil, Superconductor |
| [239](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:239) | Enum-tekster | Chemical storage of electric energy |
| [240](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:240) | Enum-tekster | Buffer battery NOTE Batteries seen as energy sources are assigned to main Class G |
| [246](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:246) | Enum-tekster | Storage of information |
| [247](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:247) | Enum-tekster | CD-ROM, EPROM, event recorder, hard disk, magnetic tape recorder, memory, RAM, video recorder, voltage recorder |
| [253](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:253) | Enum-tekster | Open storage of material at fixed location (collection, housing) |
| [254](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:254) | Enum-tekster | Bunker, cistern, paper reel stand, pit, pool |
| [260](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:260) | Enum-tekster | Closed storage of material at fixed location (collection, housing) |
| [261](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:261) | Enum-tekster | Accumulator, barrel, boiler, buffer, container, depository, flash tank, gas holder, safe, silo, tank |
| [267](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:267) | Enum-tekster | Moveable storage of material (collection, housing) |
| [268](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:268) | Enum-tekster | Container, transportation containers, gas cylinder, shipping container |
| [274](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:274) | Enum-tekster | Storage of thermal energy |
| [275](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:275) | Enum-tekster | Hot water accumulator, hybrid heat storage, ice tank, steam storage, thermal energy storage, underground thermal energy storage |
| [281](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:281) | Enum-tekster | Storage of mechanical energy |
| [282](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:282) | Enum-tekster | Flywheel, rubber band |
| [288](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:288) | Enum-tekster | Free to use |
| [295](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:295) | Enum-tekster | Cover all D codes - Customer specific codes |
| [302](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:302) | Enum-tekster | Free to use |
| [309](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:309) | Enum-tekster | Free to use |
| [316](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:316) | Enum-tekster | Cover all E codes - Supply of radiant or thermal energy |
| [317](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:317) | Enum-tekster | Cooling\nHeating\nLighting\nRadiating |
| [318](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:318) | Enum-tekster | Boiler\nElectrical heater\nHeater\nLamp\nLaser\nLuminaire |
| [325](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:325) | Enum-tekster | Electromagnetic radiation |
| [326](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:326) | Enum-tekster | Fluorescent lamp, fluorescent tube, incandescent lamp, lamp, lamp bulb, laser, LED lamp, maser, UV radiator |
| [332](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:332) | Enum-tekster | Generation of heat by conversion of electrical energy |
| [333](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:333) | Enum-tekster | Electrical boiler, electrical furnace, electrical heater, electrical radiator, electrode steam boiler, heating rod, heating wire, infra red heating element |
| [340](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:340) | Enum-tekster | Generation of cooling energy by conversion of electrical energy |
| [341](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:341) | Enum-tekster | Compression chiller, cooling unit, freezer, freezing unit, Peltier element, refrigerator, turbine-driven chiller |
| [348](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:348) | Enum-tekster | Generation of other electromagnetic radiation by means of electrical energy |
| [356](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:356) | Enum-tekster | Generation of electromagnetic radiation for lighting purposes by combustion of fossil fuels |
| [357](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:357) | Enum-tekster | Gas light, gas lamp\n |
| [364](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:364) | Enum-tekster | Generation of heat by conversion of chemical energy |
| [365](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:365) | Enum-tekster | Boiler, burner, combustion grate, furnace |
| [372](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:372) | Enum-tekster | Generation of cooling energy by conversion of chemical energy |
| [373](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:373) | Enum-tekster | Cold pumps, refrigerator |
| [380](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:380) | Enum-tekster | Generation of heat by convection |
| [381](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:381) | Enum-tekster | Boiler, condenser, evaporator, economizer, feed water heater, heat exchanger, heat recovery steam generator, radiator, steam generator |
| [388](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:388) | Enum-tekster | Generation of cooling energy by convection |
| [389](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:389) | Enum-tekster | Cold pumps, freezer, refrigerator |
| [396](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:396) | Enum-tekster | Generation of heat by conversion of mechanical energy |
| [404](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:404) | Enum-tekster | Generation of cooling energy by conversion of mechanical energy |
| [412](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:412) | Enum-tekster | Cover all F codes - Direct protection (self-acting) of a flow of energy, signals, personnel or equipment from dangerous or unwanted conditions Including systems and equipment for protective purposes Protection devices see code B. |
| [413](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:413) | Enum-tekster | Absorbing\nGuarding\nPreventing\nProtecting\nSecuring\nShielding |
| [414](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:414) | Enum-tekster | Fuse\nGuard\nMiniature circuit-breaker\nOil extension vessel\nOvervoltage limiter\nRupture disc\nSafety valve\nShielding\nSurge arrester\nThermal overload release |
| [419](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:419) | Enum-tekster | Protection against overvoltage |
| [420](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:420) | Enum-tekster | Arrester, surge arrester |
| [426](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:426) | Enum-tekster | Protection against residual current |
| [427](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:427) | Enum-tekster | Residual current device |
| [433](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:433) | Enum-tekster | Protection against overcurrent |
| [434](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:434) | Enum-tekster | Fuse, fuse unit, miniature circuit-breaker, thermal overload release |
| [440](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:440) | Enum-tekster | Protection against other electrical hazards |
| [441](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:441) | Enum-tekster | Enclosure for electromagnetic shielding, Faraday cage |
| [447](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:447) | Enum-tekster | Protection against hazardous pressure condition |
| [448](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:448) | Enum-tekster | Automatic drains trap, rupture disc, safety valve, vacuum breaker |
| [454](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:454) | Enum-tekster | Protection against effects of fire |
| [455](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:455) | Enum-tekster | Fire damper, fire protection door, fire protection facility, lock |
| [461](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:461) | Enum-tekster | Protection against hazardous operating condition or damage |
| [462](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:462) | Enum-tekster | Impact protection, protection device, protective shield, protective sleeve for thermocouple, safety clutch |
| [468](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:468) | Enum-tekster | Protection against hazardous emission (E.g. radiation, chemical emissions, noise) |
| [469](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:469) | Enum-tekster | Reactor protection equipment |
| [475](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:475) | Enum-tekster | Protection against hazards or unwanted situations for person or animals (e.g. safeguarding) |
| [476](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:476) | Enum-tekster | Airbag, barriers, contact protection, escape door, escape window, fence, gates, glare protection, guard, vision protection, railing, safety belt |
| [482](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:482) | Enum-tekster | Protection of mechanical and building technology products and systems against wear (e.g. corrosion) |
| [483](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:483) | Enum-tekster | Cathodic protection anode |
| [489](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:489) | Enum-tekster | Protection against environmental effects (e.g. weather, geophysical effects) |
| [490](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:490) | Enum-tekster | Avalanche protection device, geophysical protection device, weather protection device |
| [496](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:496) | Enum-tekster | Cover all G codes - Initiating a flow of energy or material or Generating signals used as information carriers or reference source |
| [497](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:497) | Enum-tekster | Generating |
| [498](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:498) | Enum-tekster | Dry cell battery\nFan\nFuel cell\nGenerator\nPump\nSignal generator\nSolar cell |
| [503](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:503) | Enum-tekster | Initiation of an electrical energy flow by use of mechanical energy |
| [504](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:504) | Enum-tekster | Dynamo, generator, motor-generator set, power generator, rotating generator |
| [510](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:510) | Enum-tekster | Initiation of an electrical energy flow by chemical conversion |
| [511](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:511) | Enum-tekster | Battery, dry cell battery, fuel cell |
| [517](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:517) | Enum-tekster | Initiation of an electrical energy flow using light |
| [518](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:518) | Enum-tekster | Solar cell |
| [524](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:524) | Enum-tekster | Generation of signals as an information carrier |
| [525](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:525) | Enum-tekster | Signal generator, transducer |
| [531](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:531) | Enum-tekster | Initiation of a continuous flow of solid matter |
| [532](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:532) | Enum-tekster | Belt, chain conveyor, distributor |
| [538](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:538) | Enum-tekster | Initiation of a discontinuous flow of solid matter |
| [539](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:539) | Enum-tekster | Crane, elevators, forklift, lifting gear, manipulator, lifting device |
| [545](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:545) | Enum-tekster | Initiation of a flow of liquid or flowing substances driven by an energy supply |
| [546](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:546) | Enum-tekster | Pump, screw conveyor |
| [552](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:552) | Enum-tekster | Initiation of a flow of gaseous substances by a mechanical driver |
| [553](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:553) | Enum-tekster | Aspirator, blower, compressor, fan, vacuum pump, ventilator |
| [559](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:559) | Enum-tekster | Initiation of a flow of liquid or gaseous substances by driving medium |
| [560](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:560) | Enum-tekster | Ejector, injector, jet |
| [566](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:566) | Enum-tekster | Initiation of a flow of liquid or gaseous substances by gravity |
| [567](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:567) | Enum-tekster | Lubricator, oiler |
| [573](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:573) | Enum-tekster | Cover all H codes - Producing a new kind of material or product |
| [574](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:574) | Enum-tekster | Assembling\nCrushing\nDisassembling\nFractionating\nMaterial removing\nMilling\nMixing\nProducing\nPulverizing |
| [575](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:575) | Enum-tekster | Centrifuge\nMill\nMixer\nSeparator |
| [580](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:580) | Enum-tekster | Generation of a new product by assembling |
| [581](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:581) | Enum-tekster | Assembly robot, component insertion machine |
| [587](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:587) | Enum-tekster | Separation of mixtures of substances by centrifugal force |
| [588](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:588) | Enum-tekster | Centrifuge, cyclone device |
| [594](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:594) | Enum-tekster | Separation of mixtures of substances by gravity |
| [595](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:595) | Enum-tekster | Separator, settling tank, vibrator |
| [601](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:601) | Enum-tekster | Separation of mixtures of substances by thermal processes |
| [602](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:602) | Enum-tekster | Distillation column, drying (Munters air dryer), extraction system |
| [608](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:608) | Enum-tekster | Separation of mixtures of substances by filtering or classification |
| [609](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:609) | Enum-tekster | Fluid filter, gas filter, grate, rake, screen |
| [615](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:615) | Enum-tekster | Separation of mixtures of substances by electrostatic or magnetic forces |
| [616](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:616) | Enum-tekster | Electrostatic precipitator, magnetic separator |
| [622](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:622) | Enum-tekster | Separation of mixtures of substances by physical processes |
| [623](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:623) | Enum-tekster | Absorption washer, active charcoal absorber, ion exchanger, wet ash scrubber |
| [629](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:629) | Enum-tekster | Generation of new gaseous substances |
| [630](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:630) | Enum-tekster | Gasifier |
| [636](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:636) | Enum-tekster | Generation of new form of solid material by crushing |
| [637](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:637) | Enum-tekster | Crusher, mill |
| [643](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:643) | Enum-tekster | Generation of new form of solid material by coarsening |
| [644](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:644) | Enum-tekster | Briquette maker, pellet maker, sintering facility, tablet maker |
| [650](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:650) | Enum-tekster | Generation of new substances by mixing |
| [651](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:651) | Enum-tekster | Emulsifier, humidifier (steam), kneader, mixer, mixing vessel, static mixer, stirrer |
| [657](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:657) | Enum-tekster | Generation of new substances by chemical reaction |
| [658](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:658) | Enum-tekster | Reaction furnace, reactor |
| [664](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:664) | Enum-tekster | Generation of new substances by biological reaction |
| [665](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:665) | Enum-tekster | Composter, fermenter |
| [671](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:671) | Enum-tekster | Cover all I codes - Customer specific codes |
| [678](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:678) | Enum-tekster | Free to use |
| [685](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:685) | Enum-tekster | Free to use |
| [692](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:692) | Enum-tekster | Cover all J codes - Customer specific codes |
| [699](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:699) | Enum-tekster | Free to use |
| [706](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:706) | Enum-tekster | Free to use |
| [713](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:713) | Enum-tekster | Cover all K codes - Processing (receiving, treating and providing) signals or information (excluding objects for protective purposes, see Class B or F) |
| [714](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:714) | Enum-tekster | Closing (of control circuits)\nContinuous controlling\nDelaying\nOpening (of control circuits)\nSwitching (of control circuits)\nSynchronizing |
| [715](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:715) | Enum-tekster | All-or-nothing relay\nAutomation device\nAuxiliary relay\nBlocking device\nComputer\nContactor relay\nControl device\nControl panel\nControl valve\nController\nCPU\nDelay device\nField control device\nFilter\nFilter\nInterlocking device\nOptical coupler\nSwitching fault protection device\nSynchronizing device\nTime relay\nTransistor\nTrip relay\nVoltage controller |
| [720](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:720) | Enum-tekster | Processing of electrical and electronic signals |
| [721](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:721) | Enum-tekster | All-or-nothing relay, analogue integrated circuit, automatic paralleling device, binary elements, binary integrated circuit, contactor relay, CPU, delay element, delay line, electronic valve, electronic tube, feedback controller, filter (AC or DC), induction stirrer, input/output module, microprocessor, opto-coupler, process computer, programmable controller, receiver, safety logic module, synchronizing device, time relay, transistor, transmitter |
| [727](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:727) | Enum-tekster | Processing of optical and acoustical signals |
| [728](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:728) | Enum-tekster | Mirror, controller, test unit |
| [734](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:734) | Enum-tekster | Processing of fluid and pneumatic signals |
| [735](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:735) | Enum-tekster | Controller (valve position controller), fluid feedback controller, pilot valve, valve assembly |
| [741](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:741) | Enum-tekster | Processing of mechanical signals |
| [742](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:742) | Enum-tekster | Controller, linkage |
| [748](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:748) | Enum-tekster | Processing of various input/output information carriers (e.g. electrical/pneumatic) |
| [749](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:749) | Enum-tekster | Controller, electro-hydraulic converter, electric pilot valve |
| [755](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:755) | Enum-tekster | Cover all L codes - Customer specific codes |
| [762](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:762) | Enum-tekster | Free to use |
| [769](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:769) | Enum-tekster | Free to use |
| [776](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:776) | Enum-tekster | Cover all M codes - Providing mechanical energy (rotational or linear mechanical motion) for driving purposes |
| [777](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:777) | Enum-tekster | Actuating Driving |
| [778](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:778) | Enum-tekster | Actuating coil\nActuator\nElectric motor Linear motor |
| [783](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:783) | Enum-tekster | Driving by electromagnetic force |
| [784](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:784) | Enum-tekster | Electric motor, linear motor |
| [790](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:790) | Enum-tekster | Driving by magnetic force |
| [791](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:791) | Enum-tekster | Actuating coil, actuator, electromagnet, magnetic drive |
| [797](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:797) | Enum-tekster | Driving by mechanical force |
| [798](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:798) | Enum-tekster | Friction wheel drive, mechanical actuator, spring force, stored-energy spring actuator, weight |
| [804](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:804) | Enum-tekster | Driving by hydraulic or pneumatic force |
| [805](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:805) | Enum-tekster | Fluid actuator, fluid cylinder, fluid motor, hydraulic cylinder, servomotor |
| [811](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:811) | Enum-tekster | Driving by steam flow force |
| [812](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:812) | Enum-tekster | Steam turbine |
| [818](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:818) | Enum-tekster | Driving by gas flow force |
| [819](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:819) | Enum-tekster | Gas turbine |
| [825](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:825) | Enum-tekster | Driving by wind force |
| [826](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:826) | Enum-tekster | Wind turbine |
| [832](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:832) | Enum-tekster | Driving by fluid flow force |
| [833](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:833) | Enum-tekster | Hydraulic turbine |
| [839](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:839) | Enum-tekster | Driving by force using chemical conversion means |
| [840](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:840) | Enum-tekster | Combustion engine |
| [846](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:846) | Enum-tekster | Cover all N codes - Customer specific codes |
| [853](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:853) | Enum-tekster | Free to use |
| [860](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:860) | Enum-tekster | Free to use |
| [867](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:867) | Enum-tekster | Cover all O codes - Customer specific codes |
| [874](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:874) | Enum-tekster | Free to use |
| [881](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:881) | Enum-tekster | Free to use |
| [888](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:888) | Enum-tekster | Cover all P codes - Presenting information |
| [889](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:889) | Enum-tekster | Alarming\nCommunicating\nDisplaying\nIndicating\nInforming\nMeasuring (presentation of variables)\nPresenting\nPrinting\nWarning |
| [890](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:890) | Enum-tekster | Bell\nClock\nCurrent measuring device\nEvent counter\nHorn\nIndicator\nLoudspeaker\nMechanical drop indicator relay\nMeter cabinet\nMonitor\nPower factor indicator\nPrinter\nReactive power meter\nSignal lamp\nStatus panel\nSwitching cycle counter\nSynchroscope\nText display\nTemperature gauge\nVoltmeter\nWatt-hour meter\nWattmeter |
| [895](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:895) | Enum-tekster | Visible presentation of discrete states |
| [896](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:896) | Enum-tekster | LED, mechanical drop indicator, signal lamp |
| [902](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:902) | Enum-tekster | Visible presentation of values of discrete variables |
| [903](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:903) | Enum-tekster | Ammeter, barometer, clock, counter, event counter, flow meter, frequency meter, Geiger counter, manometer, sight glass, synchroscope, thermometer, voltmeter, watt-hour meter, wattmeter, weight display |
| [909](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:909) | Enum-tekster | Visible presentation of information in drawing, pictorial and/or textual form |
| [910](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:910) | Enum-tekster | Analogue recorder, barcode printer, event recorder (mainly for presenting information), printer, recording voltmeter, text display, video screen |
| [916](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:916) | Enum-tekster | Audible presentation of information |
| [917](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:917) | Enum-tekster | Bell, horn, loudspeaker, whistle |
| [923](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:923) | Enum-tekster | Tactile presentation of information |
| [924](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:924) | Enum-tekster | Vibrator |
| [930](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:930) | Enum-tekster | Cover all Q codes - Controlled switching or varying a flow of energy, of signals (for signals in control circuits, see Classes K and S) or of material |
| [931](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:931) | Enum-tekster | Opening (of energy, signals and material flow)\nClosing (of energy, signals and material flow)\nSwitching (of energy, signals and material flow)\nClutching |
| [932](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:932) | Enum-tekster | Circuit-breaker\nContactor (for power)\nDisconnector\nEarthing switch\nFuse switch (if main purpose is protection, see Class F)\nFuse-switch-disconnector (if main purpose is protection, see Class F)\nLoad-break switch\nMotor protection switch\nMotor starter\nPower transistor\nSwitchyard\nThyristor |
| [937](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:937) | Enum-tekster | Switching and variation of electrical energy circuits |
| [938](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:938) | Enum-tekster | Circuit-breaker, contactor, motor starter, power transistor, thyristor |
| [944](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:944) | Enum-tekster | Isolation of electrical energy circuits |
| [945](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:945) | Enum-tekster | Disconnector, fuse switch, fuse-switch disconnector, isolating switch, load-break switch |
| [951](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:951) | Enum-tekster | Earthing of electrical energy circuits |
| [952](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:952) | Enum-tekster | Earthing switch |
| [958](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:958) | Enum-tekster | Braking |
| [959](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:959) | Enum-tekster | Brake |
| [965](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:965) | Enum-tekster | Switching of flow of liquid substances in closed enclosures |
| [966](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:966) | Enum-tekster | Blank, blanking plate, damper, shutoff valve (including drain valve), solenoid valve |
| [972](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:972) | Enum-tekster | Varying of flow of liquid substances in closed enclosure |
| [973](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:973) | Enum-tekster | Control damper, control valve, gas control path |
| [979](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:979) | Enum-tekster | Switching or varying of flow of liquid substances in open enclosures |
| [980](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:980) | Enum-tekster | Dam plate, lock gate |
| [986](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:986) | Enum-tekster | Providing access to an area |
| [987](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:987) | Enum-tekster | Bar (lock), cover, door, gate, lock, turnstile, window |
| [993](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:993) | Enum-tekster | Shut-off of flow of liquid substances (no valves) |
| [994](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:994) | Enum-tekster | Isolation device, rotary lock (open/close) |
| [1000](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1000) | Enum-tekster | Cover all R codes - Restricting or stabilizing motion or a flow of energy, information or material |
| [1001](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1001) | Enum-tekster | Blocking\nDamping\nRestricting\nLimiting\nStabilizing |
| [1002](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1002) | Enum-tekster | Diode\nInductor\nLimiter\nResistor\nZener diode |
| [1007](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1007) | Enum-tekster | Limiting a flow of electrical energy |
| [1008](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1008) | Enum-tekster | Arc-suppressing reactor, diode, inductor, limiter, resistor |
| [1014](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1014) | Enum-tekster | Stabilizing a flow of electrical energy |
| [1015](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1015) | Enum-tekster | Uninterruptible power supply (UPS) |
| [1021](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1021) | Enum-tekster | Stabilizing a signal |
| [1022](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1022) | Enum-tekster | Equalizer, filter, low pass |
| [1028](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1028) | Enum-tekster | Restricting an unauthorized operation and/or movement (mechanical) |
| [1029](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1029) | Enum-tekster | Blocking device, latch, lock, stop |
| [1035](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1035) | Enum-tekster | Restricting a return flow of gaseous, liquid and flowing substances |
| [1036](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1036) | Enum-tekster | Check valve |
| [1042](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1042) | Enum-tekster | Restricting a flow of liquid and gaseous substances |
| [1043](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1043) | Enum-tekster | Flow restrictor, orifice plate, Venturi nozzle, water-proof seal |
| [1049](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1049) | Enum-tekster | Restricting an emission of sound |
| [1050](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1050) | Enum-tekster | Noise protection, sound absorber |
| [1056](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1056) | Enum-tekster | Restricting a thermal flow |
| [1057](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1057) | Enum-tekster | Insulation, jacket, lagging, lining, thermal insulation louver damper |
| [1063](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1063) | Enum-tekster | Restricting a mechanical effect |
| [1064](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1064) | Enum-tekster | Brick lining, compensator, shock absorber, vibration absorption |
| [1070](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1070) | Enum-tekster | Restricting a chemical effect |
| [1071](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1071) | Enum-tekster | Brick lining, explosion protection, gas penetration protection, splash protection |
| [1077](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1077) | Enum-tekster | Restricting an emission of   light |
| [1078](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1078) | Enum-tekster | Blind, screen, shutter |
| [1084](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1084) | Enum-tekster | Restricting access to an area |
| [1085](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1085) | Enum-tekster | Fence |
| [1091](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1091) | Enum-tekster | Cover all S codes - Converting a manual operation into a signal for further processing |
| [1092](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1092) | Enum-tekster | Influencing\nManually controlling\nSelecting |
| [1093](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1093) | Enum-tekster | Control cabinet (control and operate)\nControl panel (operate)\nControl switch\nCordless mouse\nDiscrepancy switch\nKeyboard\nLight pen\nPush-button switch\nPush-button valve Selector switch\nSet-point adjuster |
| [1098](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1098) | Enum-tekster | Providing an electrical signal |
| [1099](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1099) | Enum-tekster | Control switch, discrepancy switch, keyboard, light pen, pushbutton switch, selector switch, set-point adjuster, switch |
| [1105](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1105) | Enum-tekster | Providing an electromagnetic, optical or acoustical signal |
| [1106](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1106) | Enum-tekster | Cordless mouse |
| [1112](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1112) | Enum-tekster | Providing a mechanical signal |
| [1113](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1113) | Enum-tekster | Hand wheel, selector switch |
| [1119](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1119) | Enum-tekster | Providing a fluid or pneumatic signal |
| [1120](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1120) | Enum-tekster | Push-button valve |
| [1126](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1126) | Enum-tekster | Cover all T codes - Conversion of energy maintaining the type of energy. Conversion of an established signal maintaining the content of information. Conversion of the form or shape of a material |
| [1127](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1127) | Enum-tekster | Amplifying\nModulating\nTransforming\nCasting\nCompressing\nConverting\nCutting\nMaterial deforming\nExpanding\nForging\nGrinding\nRolling\nSize enlargement\nSize reduction\nTurning |
| [1128](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1128) | Enum-tekster | Antenna\nAmplifier\nCharger\nFrequency convertor\nInterposing transformer\nInverter\nPower supply\nPower transformer\nRectifier\nSignal converter\nTest-value transmitter\nTransducer\nTransformer |
| [1133](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1133) | Enum-tekster | Converting electrical energy while retaining the energy type and energy form |
| [1134](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1134) | Enum-tekster | DC/DC converter, frequency converter, power transformer, transformer |
| [1140](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1140) | Enum-tekster | Converting electrical energy while retaining the energy type and changing the energy form |
| [1141](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1141) | Enum-tekster | Inverter, rectifier |
| [1147](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1147) | Enum-tekster | Converting signals (retention of information content) |
| [1148](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1148) | Enum-tekster | Aerial, amplifier, electrical transducer, impulse amplifier, isolating converter, signal converter |
| [1154](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1154) | Enum-tekster | Converting speed of rotation, torque, force into the same kind |
| [1155](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1155) | Enum-tekster | Automatic gear, control coupling, fluid amplifier, indexing gear, pressure amplifier, speed convertor, torque converter |
| [1161](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1161) | Enum-tekster | Converting a mechanical form with a machine |
| [1162](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1162) | Enum-tekster | Machine tool, saw, shear |
| [1168](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1168) | Enum-tekster | Converting a mechanical form by cold forming (chipless deforming) |
| [1169](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1169) | Enum-tekster | Cold drawing equipment, cold rolling equipment, deep drawing equipment |
| [1175](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1175) | Enum-tekster | Converting a mechanical form by hot forming (chipless deforming) |
| [1176](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1176) | Enum-tekster | Casting machine, extruder, forging, hot drawing equipment, hot rolling |
| [1182](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1182) | Enum-tekster | Converting radiation energy while retaining energy form |
| [1183](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1183) | Enum-tekster | Magnifying glass, parabolic mirror |
| [1189](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1189) | Enum-tekster | Cover all U codes - Keeping objects in a defined position |
| [1190](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1190) | Enum-tekster | Bearing\nCarrying\nHolding\nSupporting |
| [1191](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1191) | Enum-tekster | Assembly plate\nAssembly rail\nBearings\nBracket\nBuilding foundation\nCabinet\nCable duct\nCable framework\nCable tray\nContainer\nFixture\nGateway\nInsulator\nPipe bridge\nPole\nRoller bearing\nRoom |
| [1196](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1196) | Enum-tekster | Holding and supporting electrical energy equipment |
| [1197](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1197) | Enum-tekster | Insulator, supporting structure, transformer foundation |
| [1203](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1203) | Enum-tekster | Holding and supporting electrical energy cables and conductors |
| [1204](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1204) | Enum-tekster | Cable duct, cable rack, cable tray, cable trough, insulator, mast, portal, post insulator |
| [1210](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1210) | Enum-tekster | Enclosing and supporting electrical energy equipment |
| [1211](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1211) | Enum-tekster | Cubicle, encapsulation, housing |
| [1217](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1217) | Enum-tekster | Holding and supporting instruments, control and communication equipment |
| [1218](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1218) | Enum-tekster | Printed circuit board, sub-rack, transducer rack |
| [1224](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1224) | Enum-tekster | Holding and supporting instruments, control and communication cables and conductors |
| [1225](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1225) | Enum-tekster | Cable rack, duct, shaft |
| [1231](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1231) | Enum-tekster | Enclosing and supporting instrumentation, control and communication equipment |
| [1232](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1232) | Enum-tekster | Cabinet, cubicle, enclosures |
| [1238](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1238) | Enum-tekster | Holding and supporting machinery |
| [1239](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1239) | Enum-tekster | Machine foundation |
| [1245](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1245) | Enum-tekster | Holding and supporting structural objects |
| [1246](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1246) | Enum-tekster | Building foundation, duct (not cable duct, see UG), shaft, structural elements (e.g. column, joist, lintel, suspender beam) |
| [1252](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1252) | Enum-tekster | Holding and supporting piping objects |
| [1253](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1253) | Enum-tekster | Bracket for pipes, pipe bridge, pipe hanger |
| [1259](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1259) | Enum-tekster | Holding and guiding of shafts and rotors |
| [1260](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1260) | Enum-tekster | Ball bearing, roller bearing, sliding bearing |
| [1266](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1266) | Enum-tekster | Holding and guiding objects for manufacturing or erection |
| [1267](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1267) | Enum-tekster | Centring device, clamping, fixture |
| [1273](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1273) | Enum-tekster | Fastening and anchoring machinery |
| [1274](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1274) | Enum-tekster | Anchor plate, bracket, carrier, erection frame, erection plate |
| [1280](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1280) | Enum-tekster | Spatial objects, housing and supporting other objects |
| [1281](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1281) | Enum-tekster | Corridor, duct, hall, passage, room, shaft, stairwell |
| [1287](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1287) | Enum-tekster | Cover all V codes - Processing (treating) of material or products (including preparatory and posttreatment) |
| [1288](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1288) | Enum-tekster | Coating\nCleaning\nDehydrating\nDerusting\nDrying\nFiltering\nHeat treatment\nPacking\nPreconditioning\nRecovering\nRe-finishing\nSealing\nSeparating\nSorting\nStirring\nSurface treatment\nWrapping |
| [1289](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1289) | Enum-tekster | Separator\nFilter\nInsulator-cleaner\nOil-processing plant |
| [1294](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1294) | Enum-tekster | Filling material |
| [1295](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1295) | Enum-tekster | Drum, sack, tank car filling equipment |
| [1301](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1301) | Enum-tekster | Packaging product |
| [1302](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1302) | Enum-tekster | Packaging machine, palletizer, wrapping machines |
| [1308](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1308) | Enum-tekster | Treating surface |
| [1309](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1309) | Enum-tekster | Burnisher, grinding, painting machine, polishing machine |
| [1315](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1315) | Enum-tekster | Free to use |
| [1322](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1322) | Enum-tekster | Treating material or product |
| [1323](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1323) | Enum-tekster | Annealing furnace, balancing machine, blast furnace, melting furnace |
| [1329](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1329) | Enum-tekster | Cleaning material, product or facility |
| [1330](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1330) | Enum-tekster | Building cleaning equipment, vacuum cleaner, washing machine, |
| [1336](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1336) | Enum-tekster | Cover all W codes - Guiding or transporting energy, signals, material or products from one place to another |
| [1337](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1337) | Enum-tekster | Distributing\nGuiding\nLeading\nPositioning\nTransporting |
| [1338](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1338) | Enum-tekster | Busbar\nBushing Cable\nConductor Crane\nData bus\nOil-pipeline\nOptical fiber\nPipe\nSub-distribution board |
| [1343](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1343) | Enum-tekster | Distributing high voltage electrical energy (> 1 000 V AC or > 1 500 V DC.) |
| [1344](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1344) | Enum-tekster | Busbar |
| [1350](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1350) | Enum-tekster | Transporting high voltage electrical energy (> 1 000 V AC or > 1 500 V DC.) |
| [1351](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1351) | Enum-tekster | Bushing, cable, conductor |
| [1357](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1357) | Enum-tekster | Distributing low voltage electrical energy (≤ 1 000 V AC or ≤ 1 500 V DC) |
| [1358](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1358) | Enum-tekster | Busbar, motor control center, switchgear assembly |
| [1364](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1364) | Enum-tekster | Transporting low voltage electrical energy (≤ 1 000 V AC or ≤ 1 500 V DC) |
| [1365](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1365) | Enum-tekster | Bushing, cable, conductor |
| [1371](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1371) | Enum-tekster | Conducting earth potential or reference potential |
| [1372](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1372) | Enum-tekster | Bonding conductor, earthing busbar, earthing conductor, earth rod, shield bus |
| [1378](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1378) | Enum-tekster | Distributing electrical or electronic signal |
| [1379](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1379) | Enum-tekster | Data bus, field bus |
| [1385](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1385) | Enum-tekster | Transporting electrical or electronic signal |
| [1386](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1386) | Enum-tekster | Control cable, data line, measuring cable |
| [1392](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1392) | Enum-tekster | Transporting and routing optical signal |
| [1393](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1393) | Enum-tekster | Optical fiber, optical fiber cable, optical wave guide |
| [1399](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1399) | Enum-tekster | Transporting material or product (not driven) |
| [1400](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1400) | Enum-tekster | Conductor, inclined plane, roller table |
| [1406](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1406) | Enum-tekster | Conducting or guiding flow of substance in open enclosure |
| [1407](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1407) | Enum-tekster | Channel |
| [1413](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1413) | Enum-tekster | Conducting or guiding flow of substance in flexible, closed enclosure |
| [1414](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1414) | Enum-tekster | Hose |
| [1420](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1420) | Enum-tekster | Conducting or guiding flow of substance in rigid, closed enclosure |
| [1421](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1421) | Enum-tekster | Air duct, pipe, stack |
| [1427](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1427) | Enum-tekster | Transporting mechanical energy |
| [1428](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1428) | Enum-tekster | Chain, linkage, rotor, shaft, V-belt |
| [1434](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1434) | Enum-tekster | Conducting or guiding track-bound transport equipment |
| [1435](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1435) | Enum-tekster | Points, rails, railway, turntable |
| [1441](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1441) | Enum-tekster | Conducting or guiding persons (access equipment) |
| [1442](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1442) | Enum-tekster | Catwalk, platform, stair |
| [1448](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1448) | Enum-tekster | Conducting or guiding mobile transport equipment |
| [1449](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1449) | Enum-tekster | Path, road, shipping routes |
| [1455](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1455) | Enum-tekster | Cover all X codes - Connecting objects |
| [1456](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1456) | Enum-tekster | Connecting\nCoupling\nJoining |
| [1457](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1457) | Enum-tekster | Connector\nConverter-terminal box\nMarshalling racks\nPiping flange\nPlug connector\nSocket\nTerminal\nTerminal block\nTerminal strip |
| [1462](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1462) | Enum-tekster | Connecting high voltage objects (> 1 000 V AC or > 1 500 V DC) |
| [1463](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1463) | Enum-tekster | Terminal, junction box, socket, bushing, cable sealing box |
| [1469](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1469) | Enum-tekster | Connecting low voltage objects (≤ 1 000 V AC or ≤ 1 500 V DC) |
| [1470](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1470) | Enum-tekster | Connector, junction box, plug connector, socket-outlet, terminal, terminal block, terminal strip, cable sealing box |
| [1476](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1476) | Enum-tekster | Connecting to earth potential or reference potential |
| [1477](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1477) | Enum-tekster | Bonding terminal, earthing terminal, shield connection terminal |
| [1483](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1483) | Enum-tekster | Connecting data network carriers |
| [1484](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1484) | Enum-tekster | Hub |
| [1490](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1490) | Enum-tekster | Connecting data network carriers |
| [1491](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1491) | Enum-tekster | Connection element, plug connector, signal distributor, terminal, terminal block |
| [1497](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1497) | Enum-tekster | Connecting optical signal carriers |
| [1498](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1498) | Enum-tekster | Optical connection |
| [1504](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1504) | Enum-tekster | Connecting rigid enclosures for flows of substances |
| [1505](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1505) | Enum-tekster | Piping fitting, piping flange, piping coupling |
| [1511](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1511) | Enum-tekster | Connecting flexible enclosures for flows of substances |
| [1512](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1512) | Enum-tekster | Hose connection, hose coupling |
| [1518](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1518) | Enum-tekster | Connecting objects for transport of mechanical energy, non-detachable |
| [1519](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1519) | Enum-tekster | Rigid coupling |
| [1525](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1525) | Enum-tekster | Connecting objects for transport of mechanical energy, detachable |
| [1526](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1526) | Enum-tekster | Control coupling, disengaging coupling |
| [1532](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1532) | Enum-tekster | Connecting irreversible connected objects |
| [1533](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1533) | Enum-tekster | Bonded connection, soldered connection, welded connection |
| [1539](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1539) | Enum-tekster | Connecting reversible connected objects |
| [1540](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1540) | Enum-tekster | Hook, lug |
| [1546](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1546) | Enum-tekster | Cover all Y codes - Customer specific codes |
| [1553](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1553) | Enum-tekster | Free to use |
| [1560](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1560) | Enum-tekster | Free to use |
| [1567](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1567) | Enum-tekster | Cover all Z codes - Customer specific codes |
| [1574](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1574) | Enum-tekster | Free to use |
| [1581](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1581) | Enum-tekster | Free to use |
| [1588](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1588) | Enum-tekster | Invalid Classification |
| [1589](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ClassificationType.java:1589) | Enum-tekster | No example |

### common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java

24 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [9](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:9) | Enum-tekster | Not Checked |
| [10](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:10) | Enum-tekster | Integration between systems NOT Checked |
| [17](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:17) | Enum-tekster | No Integration |
| [18](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:18) | Enum-tekster | No Integration |
| [25](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:25) | Enum-tekster | Concept for integration identified |
| [26](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:26) | Enum-tekster | A high-level concept for integration has been identified |
| [33](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:33) | Enum-tekster | Integration Requirements specified |
| [34](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:34) | Enum-tekster | There is some level of specificity of requirements to characterize the interaction between systems |
| [41](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:41) | Enum-tekster | Detailed Integration Design defined |
| [42](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:42) | Enum-tekster | The detailed integration design has been defined to include all interface details |
| [49](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:49) | Enum-tekster | Validation of functions in lab. |
| [50](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:50) | Enum-tekster | Validation of interrelated functions between integrating systems in a laboratory environment |
| [57](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:57) | Enum-tekster | Validation of functions in relevant environment |
| [58](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:58) | Enum-tekster | Validation of interrelated functions between integrating systems in a relevant environment |
| [65](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:65) | Enum-tekster | Validation of functions in end-to-end environment |
| [66](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:66) | Enum-tekster | Validation of interrelated functions between integrating systems in a relevant end-to-end environment |
| [73](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:73) | Enum-tekster | Fully integrated system prototype demonstrated\t |
| [74](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:74) | Enum-tekster | System prototype integration demonstration in an operational high fidelity environment |
| [81](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:81) | Enum-tekster | System integration completed and qualified via tests\t |
| [82](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:82) | Enum-tekster | System integration completed and mission qualified through test and demonstration in an operational environment |
| [89](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:89) | Enum-tekster | System Integration proved in operations |
| [90](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:90) | Enum-tekster | NSystem Integration is proven through successful mission proven operations capabilities |
| [97](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:97) | Enum-tekster | Invalid IRL |
| [98](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/IrlType.java:98) | Enum-tekster | Invalid IRL |

### common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java

16 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:7) | Enum-tekster | Created |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:8) | Enum-tekster | Project has been created, but work has not yet started. |
| [13](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:13) | Enum-tekster | Planned |
| [14](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:14) | Enum-tekster | Project is planned and ready to start. |
| [19](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:19) | Enum-tekster | In progress |
| [20](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:20) | Enum-tekster | Project work is currently in progress. |
| [25](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:25) | Enum-tekster | On hold |
| [26](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:26) | Enum-tekster | Project has temporarily been put on hold. |
| [31](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:31) | Enum-tekster | At risk |
| [32](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:32) | Enum-tekster | Project is active, but one or more risks may affect delivery. |
| [37](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:37) | Enum-tekster | Completed |
| [38](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:38) | Enum-tekster | Project has been completed. |
| [43](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:43) | Enum-tekster | Cancelled |
| [44](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:44) | Enum-tekster | Project has been cancelled. |
| [49](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:49) | Enum-tekster | Archived |
| [50](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/ProjectStatus.java:50) | Enum-tekster | Project is archived and no longer active. |

### common/src/main/java/com/bepa/eis/common/enums/project/SrlType.java

11 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/SrlType.java:7) | Enum-tekster | System alternative materiel solutions should have been considered |
| [13](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/SrlType.java:13) | Enum-tekster | System materiel solution should have been identified |
| [19](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/SrlType.java:19) | Enum-tekster | System high-risk immature technologies should have been identified and prototyped |
| [25](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/SrlType.java:25) | Enum-tekster | System performance specifications and constraints should have been defined and the baseline has been allocated |
| [31](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/SrlType.java:31) | Enum-tekster | System high-risk component technology development should have been complete; low-risk system components |
| [37](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/SrlType.java:37) | Enum-tekster | System component integrability should have been validated |
| [43](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/SrlType.java:43) | Enum-tekster | System threshold capability should have been demonstrated at operational performance level using operational environment |
| [49](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/SrlType.java:49) | Enum-tekster | System interoperability should have been demonstrated in an operational environment |
| [55](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/SrlType.java:55) | Enum-tekster | System has achieved initial operational capability and can satisfy mission objectives |
| [61](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/SrlType.java:61) | Enum-tekster | Invalid SRL Level |
| [62](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/SrlType.java:62) | Enum-tekster | Invalid TRL Level |

### common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java

20 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:7) | Enum-tekster | Basic principles observed |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:8) | Enum-tekster | Identification of the new concept. Identification of the integration of the concept. Identification of expected barriers. Identification of applications. Identification of materials and technologies based on theoretical fundamentals/literature data. Preliminary numerical knowledge. Qualitative description of interactions between technologies. |
| [12](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:12) | Enum-tekster | Technology concept formulated |
| [13](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:13) | Enum-tekster | Enhanced knowledge of technologies, materials and interfaces is acquired. New concept is investigated and refined. First evaluation about the feasibility is performed. Initial numerical knowledge. Qualitative description of interactions between technologies. |
| [17](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:17) | Enum-tekster | Experimental proof of concept |
| [18](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:18) | Enum-tekster | Prototype of the new concept is developed. Numerical knowledge is acquired. Qualitative description of interactions between technologies. |
| [22](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:22) | Enum-tekster | Technology validated in lab |
| [23](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:23) | Enum-tekster | Prototype of the new concept is validated in relevant environment. Numerical knowledge is acquired. Qualitative description of interactions between technologies. |
| [27](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:27) | Enum-tekster | Technology validated in relevant environment |
| [28](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:28) | Enum-tekster | Prototype of the new concept is validated in relevant environment. Numerical knowledge is acquired. Qualitative description of interactions between technologies. |
| [32](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:32) | Enum-tekster | Technology pilot demonstrated in relevant environment |
| [33](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:33) | Enum-tekster | Prototype of the new concept is validated in relevant environment. Numerical knowledge is acquired. Qualitative description of interactions between technologies. |
| [37](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:37) | Enum-tekster | System prototype demonstration in operational environment |
| [38](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:38) | Enum-tekster | Prototype of the new concept is validated in relevant environment. Numerical knowledge is acquired. Qualitative description of interactions between technologies. |
| [42](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:42) | Enum-tekster | System complete and qualified |
| [43](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:43) | Enum-tekster | Prototype of the new concept is validated in relevant environment. Numerical knowledge is acquired. Qualitative description of interactions between technologies. |
| [47](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:47) | Enum-tekster | Actual system proven in operational environment |
| [48](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:48) | Enum-tekster | Prototype of the new concept is validated in relevant environment. Numerical knowledge is acquired. Qualitative description of interactions between technologies. |
| [52](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:52) | Enum-tekster | Invalid TRL Level |
| [53](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/project/TrlType.java:53) | Enum-tekster | Invalid TRL Level |

### common/src/main/java/com/bepa/eis/common/enums/user/UserRoles.java

10 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [7](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/user/UserRoles.java:7) | Enum-tekster | Bepa system administrator |
| [8](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/user/UserRoles.java:8) | Enum-tekster | Full access to the system. |
| [15](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/user/UserRoles.java:15) | Enum-tekster | Administrator |
| [16](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/user/UserRoles.java:16) | Enum-tekster | Administrator with access to the customer portal. |
| [23](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/user/UserRoles.java:23) | Enum-tekster | Project Member |
| [24](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/user/UserRoles.java:24) | Enum-tekster | Project member with read/write access to project data |
| [31](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/user/UserRoles.java:31) | Enum-tekster | Project viewer |
| [32](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/user/UserRoles.java:32) | Enum-tekster | Project member with only read access to project data |
| [39](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/user/UserRoles.java:39) | Enum-tekster | Invalid User Role |
| [40](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/enums/user/UserRoles.java:40) | Enum-tekster | No access to the system. |

### common/src/main/java/com/bepa/eis/common/providers/EisDataSourceProvider.java

5 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [27](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/EisDataSourceProvider.java:27) | Fejl og exceptions | Unsupported database datasource mode:  |
| [28](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/EisDataSourceProvider.java:28) | Fejl og exceptions | . Expected 'jndi' or 'jdbc'. |
| [39](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/EisDataSourceProvider.java:39) | Fejl og exceptions | Failed to lookup DataSource via JNDI name:  |
| [72](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/EisDataSourceProvider.java:72) | Fejl og exceptions | Database datasource mode is jdbc, but database.jdbc.url is empty for application:  |
| [79](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/EisDataSourceProvider.java:79) | Fejl og exceptions | Database datasource mode is jdbc, but database.jdbc.username is empty for application:  |

### common/src/main/java/com/bepa/eis/common/providers/SessionProvider.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [194](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/SessionProvider.java:194) | Fejl og exceptions | No session found for sessionId:  |

### common/src/main/java/com/bepa/eis/common/providers/UserPreferenceProvider.java

5 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [124](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserPreferenceProvider.java:124) | Fejl og exceptions | userId is required |
| [171](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserPreferenceProvider.java:171) | Fejl og exceptions | userId is required |
| [174](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserPreferenceProvider.java:174) | Fejl og exceptions | key is required |
| [242](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserPreferenceProvider.java:242) | Fejl og exceptions | Could not parse user preferences XML |
| [258](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserPreferenceProvider.java:258) | Fejl og exceptions | Could not serialize user preferences XML |

### common/src/main/java/com/bepa/eis/common/providers/UserProvider.java

43 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [528](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:528) | Øvrige tekstkandidater | Missing email |
| [532](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:532) | Øvrige tekstkandidater | Missing password |
| [542](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:542) | Øvrige tekstkandidater | Unknown user |
| [550](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:550) | Øvrige tekstkandidater | User is inactive |
| [556](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:556) | Øvrige tekstkandidater | Account is locked |
| [560](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:560) | Øvrige tekstkandidater | Invalid password |
| [567](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:567) | Øvrige tekstkandidater | Login validation error |
| [696](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:696) | Øvrige tekstkandidater | MFA could not be enabled because userId was missing |
| [707](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:707) | Øvrige tekstkandidater | MFA could not be enabled because MFA secret was missing |
| [726](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:726) | Øvrige tekstkandidater | User enabled two-factor authentication |
| [734](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:734) | Øvrige tekstkandidater | MFA setup was verified, but no user row was updated |
| [747](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:747) | Øvrige tekstkandidater | MFA could not be enabled due to a database error |
| [770](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:770) | Øvrige tekstkandidater | MFA reset failed because user was not found |
| [793](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:793) | Øvrige tekstkandidater | MFA reset failed because userId was missing |
| [817](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:817) | Øvrige tekstkandidater | MFA was reset by administrator |
| [838](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:838) | Øvrige tekstkandidater | MFA reset failed due to a database error |
| [855](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:855) | Øvrige tekstkandidater | MFA disable failed because userId was missing |
| [873](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:873) | Øvrige tekstkandidater | MFA was disabled for user |
| [894](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:894) | Øvrige tekstkandidater | MFA disable failed due to a database error |
| [921](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:921) | Øvrige tekstkandidater | User MFA policy could not be changed because user was not found |
| [942](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:942) | Øvrige tekstkandidater | User MFA policy could not be changed because input was missing |
| [961](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:961) | Øvrige tekstkandidater | User MFA policy changed to  |
| [982](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:982) | Øvrige tekstkandidater | User MFA policy change failed due to a database error |
| [1004](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1004) | Øvrige tekstkandidater | MFA reset-required flag could not be set because userId was missing |
| [1028](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1028) | Øvrige tekstkandidater | User was marked as requiring MFA reset |
| [1049](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1049) | Øvrige tekstkandidater | MFA reset-required flag failed due to a database error |
| [1066](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1066) | Øvrige tekstkandidater | MFA reset-required flag could not be cleared because userId was missing |
| [1084](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1084) | Øvrige tekstkandidater | MFA reset-required flag was cleared |
| [1105](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1105) | Øvrige tekstkandidater | MFA reset-required flag clear failed due to a database error |
| [1303](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1303) | Fejl og exceptions | Name is required. |
| [1307](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1307) | Fejl og exceptions | Email is required. |
| [1319](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1319) | Fejl og exceptions | Customer ID is required. |
| [1349](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1349) | Øvrige tekstkandidater | User administration data saved |
| [1365](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1365) | Øvrige tekstkandidater | User administration data could not be saved |
| [1380](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1380) | Fejl og exceptions | Another active user already uses this email. |
| [1395](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1395) | Fejl og exceptions | Another active user already uses this email. |
| [1693](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1693) | Øvrige tekstkandidater | Welcome email queued |
| [1693](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1693) | Øvrige tekstkandidater | Welcome email could not be queued |
| [1694](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1694) | Øvrige tekstkandidater | Password reset link queued |
| [1694](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1694) | Øvrige tekstkandidater | Password reset link could not be queued |
| [1740](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1740) | Øvrige tekstkandidater | Password reset completed |
| [1834](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1834) | Fejl og exceptions | Customer ID is required. |
| [1880](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/UserProvider.java:1880) | Fejl og exceptions | User has more than one customer relation. |

### common/src/main/java/com/bepa/eis/common/providers/customer/CustomerPaymentRequestProvider.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [186](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerPaymentRequestProvider.java:186) | Øvrige tekstkandidater | Payment requested. |

### common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java

24 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [68](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:68) | Øvrige tekstkandidater | Customer registration data is required. |
| [78](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:78) | Øvrige tekstkandidater | Email is already used by an active user. |
| [84](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:84) | Øvrige tekstkandidater | No active subscription plan was found for the selected subscription. |
| [88](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:88) | Øvrige tekstkandidater | Selected billing period is not available for the chosen subscription. |
| [97](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:97) | Øvrige tekstkandidater | Selected billing period could not be resolved. |
| [106](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:106) | Øvrige tekstkandidater | Customer could not be created. |
| [123](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:123) | Øvrige tekstkandidater | Customer was created, but user could not be created for email:  |
| [140](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:140) | Øvrige tekstkandidater | Customer and user were created, but user could not be linked to customer. |
| [177](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:177) | Øvrige tekstkandidater | Customer onboarding workflow started after customer registration. |
| [194](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:194) | Øvrige tekstkandidater | Customer registration completed. |
| [263](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:263) | Øvrige tekstkandidater | Subscription is required. |
| [267](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:267) | Øvrige tekstkandidater | Billing period is required. |
| [271](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:271) | Øvrige tekstkandidater | Customer name is required. |
| [275](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:275) | Øvrige tekstkandidater | Address is required. |
| [279](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:279) | Øvrige tekstkandidater | Zip code is required. |
| [283](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:283) | Øvrige tekstkandidater | City is required. |
| [287](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:287) | Øvrige tekstkandidater | Country is required. |
| [291](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:291) | Øvrige tekstkandidater | Phone is required. |
| [295](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:295) | Øvrige tekstkandidater | Contact email is required. |
| [299](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:299) | Øvrige tekstkandidater | Cardholder name is required. |
| [303](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:303) | Øvrige tekstkandidater | Card number is required. |
| [307](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:307) | Øvrige tekstkandidater | Expiry month is required. |
| [311](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:311) | Øvrige tekstkandidater | Expiry year is required. |
| [315](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerRegistrationProvider.java:315) | Øvrige tekstkandidater | Billing zip code is required. |

### common/src/main/java/com/bepa/eis/common/providers/customer/CustomerTokenProvider.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [307](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerTokenProvider.java:307) | Fejl og exceptions | Could not hash customer token. |

### common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowActionProvider.java

11 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [60](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowActionProvider.java:60) | Øvrige tekstkandidater | Customer email confirmed. |
| [72](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowActionProvider.java:72) | Øvrige tekstkandidater | Customer confirmed subscription continuation. |
| [84](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowActionProvider.java:84) | Øvrige tekstkandidater | Customer confirmed subscription renewal. |
| [96](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowActionProvider.java:96) | Øvrige tekstkandidater | Customer reactivated. |
| [117](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowActionProvider.java:117) | Øvrige tekstkandidater | Token is missing. |
| [123](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowActionProvider.java:123) | Øvrige tekstkandidater | Token was not found. |
| [127](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowActionProvider.java:127) | Øvrige tekstkandidater | Token type is invalid. |
| [133](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowActionProvider.java:133) | Øvrige tekstkandidater | Token is expired or already used. |
| [139](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowActionProvider.java:139) | Øvrige tekstkandidater | Workflow was not found. |
| [156](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowActionProvider.java:156) | Øvrige tekstkandidater | Payment request could not be created. |
| [186](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowActionProvider.java:186) | Øvrige tekstkandidater | Workflow could not be updated. |

### common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowMaintenanceProvider.java

7 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [24](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowMaintenanceProvider.java:24) | SQL med gemt fejltekst |         WHEN LastError IS NULL OR LTRIM(RTRIM(LastError)) = '' THEN 'Workflow lock timed out and was released automatically'  |
| [49](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowMaintenanceProvider.java:49) | SQL med gemt fejltekst |     LastError = 'Customer email confirmation expired',  |
| [73](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowMaintenanceProvider.java:73) | Øvrige tekstkandidater | release active stuck customer workflow locks |
| [79](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowMaintenanceProvider.java:79) | Øvrige tekstkandidater | release terminal stuck customer workflow locks |
| [100](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowMaintenanceProvider.java:100) | Øvrige tekstkandidater | cancel expired pending email confirmation workflows |
| [137](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowMaintenanceProvider.java:137) | Fejl og exceptions | Could not  |
| [151](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowMaintenanceProvider.java:151) | Fejl og exceptions | Could not  |

### common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowProvider.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [277](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowProvider.java:277) | Fejl og exceptions | Could not load due customer workflows |
| [371](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowProvider.java:371) | Øvrige tekstkandidater | Unknown customer workflow error |

### common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowStarterProvider.java

3 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [57](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowStarterProvider.java:57) | Øvrige tekstkandidater | Customer onboarding workflow started. |
| [68](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowStarterProvider.java:68) | Øvrige tekstkandidater | Customer onboarding workflow started. |
| [114](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/CustomerWorkflowStarterProvider.java:114) | Øvrige tekstkandidater | Customer onboarding workflow started. |

### common/src/main/java/com/bepa/eis/common/providers/customer/SubscriptionPlanBillingPeriodProvider.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [257](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/customer/SubscriptionPlanBillingPeriodProvider.java:257) | Fejl og exceptions | Could not read generated billing period id. |

### common/src/main/java/com/bepa/eis/common/providers/mail/MailProvider.java

11 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [169](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailProvider.java:169) | SQL med gemt fejltekst | LastError = 'Mail sending lock timed out',  |
| [538](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailProvider.java:538) | Fejl og exceptions | Could not load pending mails |
| [600](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailProvider.java:600) | Øvrige tekstkandidater | Unknown mail delivery error |
| [622](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailProvider.java:622) | Øvrige tekstkandidater | Mail could not be delivered |
| [643](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailProvider.java:643) | Øvrige tekstkandidater | Mail cancelled |
| [664](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailProvider.java:664) | Fejl og exceptions | Could not release stuck sending mails |
| [689](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailProvider.java:689) | Øvrige tekstkandidater | sent mails last 7 days |
| [693](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailProvider.java:693) | Øvrige tekstkandidater | mail errors last 7 days |
| [697](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailProvider.java:697) | Øvrige tekstkandidater | failed mails last 7 days |
| [701](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailProvider.java:701) | Øvrige tekstkandidater | undelivered mails last 7 days |
| [901](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailProvider.java:901) | Øvrige tekstkandidater | sent mails last 24 hours |

### common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java

15 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [11](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:11) | Øvrige tekstkandidater | Example Ltd |
| [12](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:12) | Øvrige tekstkandidater | Anne Jensen |
| [14](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:14) | Øvrige tekstkandidater | 20 Oct 2026, 12:00 |
| [16](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:16) | Øvrige tekstkandidater | 1 Nov 2026 |
| [17](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:17) | Øvrige tekstkandidater | 8 Nov 2026 |
| [17](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:17) | Øvrige tekstkandidater | The payment was declined |
| [19](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:19) | Øvrige tekstkandidater | The payment deadline has passed |
| [20](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:20) | Øvrige tekstkandidater | 10 Oct 2026, 12:00 |
| [20](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:20) | Øvrige tekstkandidater | 12 Oct 2026, 09:00 |
| [21](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:21) | Øvrige tekstkandidater | 1 Nov 2026 |
| [22](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:22) | Øvrige tekstkandidater | 1 year |
| [22](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:22) | Øvrige tekstkandidater | 20 Oct 2026 |
| [23](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:23) | Øvrige tekstkandidater | 25 Oct 2026 |
| [23](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:23) | Øvrige tekstkandidater | 1 Oct 2026 |
| [24](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateExamples.java:24) | Øvrige tekstkandidater | 31 Oct 2026 |

### common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateLogo.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [38](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateLogo.java:38) | Fejl og exceptions | Missing mail PNG logo resource |
| [40](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateLogo.java:40) | Fejl og exceptions | Could not read mail PNG logo |

### common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateRenderer.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [21](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateRenderer.java:21) | Fejl og exceptions | Database mail template not found:  |
| [23](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/MailTemplateRenderer.java:23) | Fejl og exceptions | Could not load database mail template:  |

### common/src/main/java/com/bepa/eis/common/providers/mail/SmtpMailSender.java

9 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [28](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/SmtpMailSender.java:28) | Øvrige tekstkandidater | Mail item is missing. |
| [64](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/SmtpMailSender.java:64) | Øvrige tekstkandidater | Failed to send mail:  |
| [78](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/SmtpMailSender.java:78) | Øvrige tekstkandidater | SMTP error while sending mail:  |
| [91](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/SmtpMailSender.java:91) | Øvrige tekstkandidater | Unexpected error while sending mail:  |
| [193](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/SmtpMailSender.java:193) | Øvrige tekstkandidater | Sender email is missing. |
| [197](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/SmtpMailSender.java:197) | Øvrige tekstkandidater | Recipient email is missing. |
| [201](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/SmtpMailSender.java:201) | Øvrige tekstkandidater | Mail subject is missing. |
| [205](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/SmtpMailSender.java:205) | Øvrige tekstkandidater | Mail body is missing. |
| [227](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/mail/SmtpMailSender.java:227) | Øvrige tekstkandidater | Unknown error |

### common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java

19 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [36](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:36) | Øvrige tekstkandidater | Execution observation |
| [51](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:51) | Øvrige tekstkandidater | Invalid log cleanup configuration |
| [59](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:59) | Øvrige tekstkandidater | Invalid cleanup job configuration |
| [62](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:62) | Øvrige tekstkandidater | Mail queue job |
| [63](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:63) | Øvrige tekstkandidater | Customer workflow job |
| [64](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:64) | Øvrige tekstkandidater | SMTP email |
| [65](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:65) | Øvrige tekstkandidater | CVR lookup |
| [66](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:66) | Øvrige tekstkandidater | VIES VAT validation |
| [67](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:67) | Øvrige tekstkandidater | Geo-IP lookup |
| [78](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:78) | Øvrige tekstkandidater | No execution recorded |
| [100](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:100) | Øvrige tekstkandidater | Invalid cron job configuration |
| [105](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:105) | Øvrige tekstkandidater | No execution recorded |
| [115](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:115) | Korte tekstkandidater | Critical |
| [115](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:115) | Korte tekstkandidater | Unknown |
| [117](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:117) | Øvrige tekstkandidater | Job has missed its expected interval |
| [117](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:117) | Øvrige tekstkandidater | No observation in the last 24 hours |
| [118](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:118) | Korte tekstkandidater | Warning |
| [118](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:118) | Øvrige tekstkandidater | Job completed with workflow errors |
| [119](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/ComponentHealthProvider.java:119) | Øvrige tekstkandidater | Last execution  |

### common/src/main/java/com/bepa/eis/common/providers/misc/DataCleanupProvider.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [13](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/DataCleanupProvider.java:13) | Fejl og exceptions | Invalid retention days |
| [29](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/DataCleanupProvider.java:29) | Fejl og exceptions | Cleanup interrupted |

### common/src/main/java/com/bepa/eis/common/providers/misc/EventProvider.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [95](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/EventProvider.java:95) | Fejl og exceptions | Insert event failed :  |

### common/src/main/java/com/bepa/eis/common/providers/misc/IncidentProvider.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [381](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/IncidentProvider.java:381) | Fejl og exceptions | Insert incident failed :  |

### common/src/main/java/com/bepa/eis/common/providers/misc/LogArchiveCleanup.java

5 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [17](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/LogArchiveCleanup.java:17) | Fejl og exceptions | Invalid retention days |
| [28](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/LogArchiveCleanup.java:28) | Fejl og exceptions | Invalid log archive directory:  |
| [41](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/LogArchiveCleanup.java:41) | Fejl og exceptions | Log file outside configured directory |
| [48](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/LogArchiveCleanup.java:48) | Øvrige tekstkandidater | Some log archives could not be deleted |
| [54](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/LogArchiveCleanup.java:54) | Øvrige tekstkandidater | Log archive cleanup failed |

### common/src/main/java/com/bepa/eis/common/providers/misc/PerformanceProvider.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [119](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/misc/PerformanceProvider.java:119) | Fejl og exceptions | Insert performance failed :  |

### common/src/main/java/com/bepa/eis/common/providers/security/GeoIpService.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [46](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/security/GeoIpService.java:46) | Øvrige tekstkandidater | Local network |
| [57](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/security/GeoIpService.java:57) | Øvrige tekstkandidater | Private network |

### common/src/main/java/com/bepa/eis/common/providers/security/MfaTotpService.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [212](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/security/MfaTotpService.java:212) | Fejl og exceptions | Invalid Base32 character:  |

### common/src/main/java/com/bepa/eis/common/providers/security/PasswordHasher.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [23](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/security/PasswordHasher.java:23) | Fejl og exceptions | Password must not be blank |
| [79](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/security/PasswordHasher.java:79) | Fejl og exceptions | PBKDF2WithHmacSHA256 is unavailable |

### common/src/main/java/com/bepa/eis/common/providers/security/SessionManager.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [70](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/security/SessionManager.java:70) | Øvrige tekstkandidater | Login session was created |
| [79](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/security/SessionManager.java:79) | Øvrige tekstkandidater | Login session could not be created |
| [100](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/security/SessionManager.java:100) | Øvrige tekstkandidater | User logged out |
| [109](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/security/SessionManager.java:109) | Øvrige tekstkandidater | User logout failed |
| [130](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/security/SessionManager.java:130) | Øvrige tekstkandidater | Session expired |
| [139](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/providers/security/SessionManager.java:139) | Øvrige tekstkandidater | Session expiration failed |

### common/src/main/java/com/bepa/eis/common/scheduling/CleanupJobSettings.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [32](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/scheduling/CleanupJobSettings.java:32) | Fejl og exceptions | retention.days must be between 1 and 36500 |

### common/src/main/java/com/bepa/eis/common/scheduling/CronJobSettings.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [10](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/scheduling/CronJobSettings.java:10) | Fejl og exceptions | .enabled must be true or false |

### common/src/main/java/com/bepa/eis/common/scheduling/CronSchedule.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [14](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/scheduling/CronSchedule.java:14) | Fejl og exceptions | Cron requires five fields: minute hour day month weekday |
| [46](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/scheduling/CronSchedule.java:46) | Fejl og exceptions | Cron has no occurrence in the next eight years |
| [59](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/scheduling/CronSchedule.java:59) | Fejl og exceptions | Invalid cron field:  |
| [61](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/scheduling/CronSchedule.java:61) | Fejl og exceptions | Invalid cron step:  |
| [67](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/scheduling/CronSchedule.java:67) | Fejl og exceptions | Invalid cron range:  |
| [71](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/scheduling/CronSchedule.java:71) | Fejl og exceptions | Cron field out of range:  |

### common/src/main/java/com/bepa/eis/common/scheduling/LogCleanupSettings.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [9](/C:/udv/EIS/common/src/main/java/com/bepa/eis/common/scheduling/LogCleanupSettings.java:9) | Øvrige tekstkandidater | Archived log cleanup job |

### integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java

37 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [173](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:173) | Øvrige tekstkandidater | Customer workflow processing failed:  |
| [199](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:199) | Øvrige tekstkandidater | Customer workflow has no current state. |
| [226](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:226) | Øvrige tekstkandidater | Unsupported customer workflow state:  |
| [246](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:246) | Fejl og exceptions | Email confirmation token could not be created. |
| [276](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:276) | Øvrige tekstkandidater | Customer workflow started and email confirmation is required. |
| [290](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:290) | Øvrige tekstkandidater | Customer email confirmation expired before the customer confirmed the account. |
| [296](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:296) | Øvrige tekstkandidater | Customer workflow is waiting for email confirmation. |
| [303](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:303) | Fejl og exceptions | Trial subscription could not be created. |
| [317](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:317) | Øvrige tekstkandidater | Customer email has been confirmed and trial has started. |
| [326](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:326) | Øvrige tekstkandidater | Trial workflow has no subscription record. |
| [331](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:331) | Øvrige tekstkandidater | Trial expired before continuation was confirmed. |
| [338](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:338) | Øvrige tekstkandidater | Trial expired before continuation was confirmed. |
| [351](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:351) | Øvrige tekstkandidater | Trial period is close to expiry. |
| [360](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:360) | Øvrige tekstkandidater | Trial expiring workflow has no subscription record. |
| [393](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:393) | Øvrige tekstkandidater | Trial expiration reminder was processed and subscription continuation confirmation is required. |
| [402](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:402) | Øvrige tekstkandidater | Subscription continuation was not confirmed before trial expiry. |
| [409](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:409) | Øvrige tekstkandidater | Subscription continuation was not confirmed before trial expiry. |
| [415](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:415) | Øvrige tekstkandidater | Customer workflow is waiting for subscription continuation confirmation. |
| [423](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:423) | Øvrige tekstkandidater | Customer workflow is waiting for payment creation or callback. |
| [435](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:435) | Øvrige tekstkandidater | Payment succeeded and subscription has been activated. |
| [450](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:450) | Øvrige tekstkandidater | Payment failed and is now overdue. |
| [457](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:457) | Øvrige tekstkandidater | Customer workflow is waiting for payment confirmation. |
| [465](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:465) | Øvrige tekstkandidater | Customer workflow is active but subscription period is not yet available. |
| [479](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:479) | Øvrige tekstkandidater | Subscription is close to expiry. |
| [513](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:513) | Øvrige tekstkandidater | Customer workflow is waiting for subscription renewal confirmation. |
| [527](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:527) | Øvrige tekstkandidater | Overdue payment was completed successfully and subscription has been activated. |
| [534](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:534) | Øvrige tekstkandidater | Payment grace period expired. |
| [541](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:541) | Øvrige tekstkandidater | Payment grace period expired. |
| [563](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:563) | Øvrige tekstkandidater | Customer workflow is waiting for overdue payment to be completed. |
| [567](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:567) | Øvrige tekstkandidater | Customer workflow is suspended. |
| [572](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:572) | Øvrige tekstkandidater | Customer workflow is suspended. |
| [593](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:593) | Øvrige tekstkandidater | Customer workflow is suspended. |
| [615](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:615) | Øvrige tekstkandidater | Customer workflow is cancelled. |
| [635](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:635) | Øvrige tekstkandidater | Customer workflow is waiting for manual attention. |
| [819](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:819) | Fejl og exceptions | Customer workflow state could not be updated. workflowId= |
| [850](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:850) | Fejl og exceptions | Customer workflow could not be moved to manual attention. workflowId= |
| [930](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/CustomerWorkflowProcessor.java:930) | Øvrige tekstkandidater | Unknown customer workflow error |

### integration-server/src/main/java/com/bepa/eis/integration/IntegrationServerApplication.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [34](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/IntegrationServerApplication.java:34) | Øvrige tekstkandidater | Mail queue job |
| [40](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/IntegrationServerApplication.java:40) | Øvrige tekstkandidater | Customer workflow job |

### integration-server/src/main/java/com/bepa/eis/integration/MailQueueJob.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [128](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/MailQueueJob.java:128) | Øvrige tekstkandidater | Mail sender returned no result. |
| [210](/C:/udv/EIS/integration-server/src/main/java/com/bepa/eis/integration/MailQueueJob.java:210) | Øvrige tekstkandidater | Unknown mail queue job error |

### timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java

40 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [125](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:125) | Fejl og exceptions | year must be between 1 and 9998 |
| [141](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:141) | Øvrige tekstkandidater | Customer not found |
| [148](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:148) | Øvrige tekstkandidater | Activity not found |
| [155](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:155) | Øvrige tekstkandidater | Time entry not found |
| [162](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:162) | Øvrige tekstkandidater | Material not found |
| [177](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:177) | Øvrige tekstkandidater | Customer not found |
| [179](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:179) | Fejl og exceptions | The selected customer is inactive |
| [251](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:251) | Øvrige tekstkandidater | Customer not found |
| [263](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:263) | Øvrige tekstkandidater | Activity not found |
| [272](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:272) | Øvrige tekstkandidater | Time entry not found |
| [283](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:283) | Øvrige tekstkandidater | Material not found |
| [327](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:327) | Øvrige tekstkandidater | Customer not found |
| [373](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:373) | Øvrige tekstkandidater | Customer not found |
| [434](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:434) | Fejl og exceptions | fromMonth must be between 1 and 12 |
| [437](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:437) | Fejl og exceptions | toMonth must be between 1 and 12 |
| [489](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:489) | Korte tekstkandidater | Firmanavn |
| [490](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:490) | Øvrige tekstkandidater | Kontakt navn |
| [491](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:491) | Øvrige tekstkandidater | Kontakt e-mail |
| [493](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:493) | Korte tekstkandidater | Adresse |
| [494](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:494) | Korte tekstkandidater | Postnr. |
| [495](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:495) | Korte tekstkandidater | By |
| [508](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:508) | Øvrige tekstkandidater | Kort aktivitetsbeskrivelse |
| [520](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:520) | Korte tekstkandidater | Aktivitet |
| [521](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:521) | Korte tekstkandidater | Dato |
| [536](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:536) | Korte tekstkandidater | Dato |
| [538](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:538) | Korte tekstkandidater | Enhed |
| [539](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:539) | Korte tekstkandidater | Beskrivelse |
| [549](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:549) | Fejl og exceptions |  is required |
| [556](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:556) | Fejl og exceptions |  is required |
| [563](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:563) | Fejl og exceptions |  is required |
| [574](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:574) | Fejl og exceptions | Hours are required |
| [578](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:578) | Fejl og exceptions | Hours must be in 0.5 hour increments |
| [588](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:588) | Fejl og exceptions | Activity is required |
| [591](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:591) | Øvrige tekstkandidater | Activity not found |
| [593](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:593) | Fejl og exceptions | The activity does not belong to the selected customer |
| [596](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:596) | Fejl og exceptions | The selected activity is inactive |
| [630](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:630) | Fejl og exceptions | Parameter is required:  |
| [638](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:638) | Fejl og exceptions | Parameter is required:  |
| [670](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:670) | Svar og validering | Not found |
| [677](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/api/ApiServlet.java:677) | Øvrige tekstkandidater | Unknown error |

### timesheet-app/src/main/java/dk/eis/tech/timesheet/config/CompanyFooterConfig.java

8 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [14](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/config/CompanyFooterConfig.java:14) | Øvrige tekstkandidater | EIS Technology |
| [16](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/config/CompanyFooterConfig.java:16) | Øvrige tekstkandidater | Haslehøjvej 15 |
| [17](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/config/CompanyFooterConfig.java:17) | Øvrige tekstkandidater | 8210 Aarhus V |
| [21](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/config/CompanyFooterConfig.java:21) | Øvrige tekstkandidater | Phone: 2171 7344 |
| [22](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/config/CompanyFooterConfig.java:22) | Øvrige tekstkandidater | E-mail: georg.almasy@gmail.com |
| [23](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/config/CompanyFooterConfig.java:23) | Øvrige tekstkandidater | CVR no.: 29453268 |
| [27](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/config/CompanyFooterConfig.java:27) | Øvrige tekstkandidater | Jyske Bank |
| [28](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/config/CompanyFooterConfig.java:28) | Øvrige tekstkandidater | Silkeborg afd. |

### timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java

20 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [122](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:122) | Fejl og exceptions | Unable to create account |
| [232](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:232) | Øvrige tekstkandidater | Entry not found |
| [241](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:241) | Øvrige tekstkandidater | Correction of entry  |
| [247](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:247) | Øvrige tekstkandidater | Correction reversal for entry  |
| [289](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:289) | Fejl og exceptions | Invoice is already approved |
| [315](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:315) | Fejl og exceptions | Unable to approve invoice |
| [372](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:372) | Øvrige tekstkandidater | Entry type |
| [381](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:381) | Fejl og exceptions | Unable to create accounting entry |
| [392](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:392) | Øvrige tekstkandidater | Line text |
| [457](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:457) | Fejl og exceptions | Account not found |
| [461](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:461) | Fejl og exceptions | At least two accounting lines are required |
| [468](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:468) | Fejl og exceptions | Each line must have either debit or credit amount |
| [473](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:473) | Fejl og exceptions | Debit and credit must balance |
| [477](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:477) | Øvrige tekstkandidater | Account number |
| [478](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:478) | Øvrige tekstkandidater | Account name |
| [479](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:479) | Øvrige tekstkandidater | Account type |
| [513](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:513) | Fejl og exceptions | Required account is missing or inactive:  |
| [518](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:518) | Fejl og exceptions |  is required |
| [523](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:523) | Fejl og exceptions | Entry date is required |
| [528](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/AccountingRepository.java:528) | Fejl og exceptions |  is required |

### timesheet-app/src/main/java/dk/eis/tech/timesheet/data/ActivityRepository.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [68](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/ActivityRepository.java:68) | Fejl og exceptions | Unable to create activity |

### timesheet-app/src/main/java/dk/eis/tech/timesheet/data/CustomerRepository.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [69](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/CustomerRepository.java:69) | Fejl og exceptions | Unable to create customer |

### timesheet-app/src/main/java/dk/eis/tech/timesheet/data/Database.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [22](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/Database.java:22) | Fejl og exceptions | Unable to open JNDI connection |

### timesheet-app/src/main/java/dk/eis/tech/timesheet/data/MaterialRepository.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [76](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/MaterialRepository.java:76) | Fejl og exceptions | Unable to create material entry |

### timesheet-app/src/main/java/dk/eis/tech/timesheet/data/TimeEntryRepository.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [102](/C:/udv/EIS/timesheet-app/src/main/java/dk/eis/tech/timesheet/data/TimeEntryRepository.java:102) | Fejl og exceptions | Unable to create time entry |

### web-server/src/main/java/com/bepa/eis/server/api/DTO/User.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [19](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/DTO/User.java:19) | Indlejret HTML/XML | <Unknown User> |

### web-server/src/main/java/com/bepa/eis/server/api/external/vies/ViesVatLookupService.java

7 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [69](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/vies/ViesVatLookupService.java:69) | Fejl og exceptions | Country code must contain exactly 2 letters. |
| [73](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/vies/ViesVatLookupService.java:73) | Fejl og exceptions | VAT number format is invalid. |
| [96](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/vies/ViesVatLookupService.java:96) | Fejl og exceptions | VIES returned HTTP  |
| [144](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/vies/ViesVatLookupService.java:144) | Fejl og exceptions | VIES request was interrupted. |
| [153](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/vies/ViesVatLookupService.java:153) | Fejl og exceptions | Could not call VIES. |
| [163](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/vies/ViesVatLookupService.java:163) | Fejl og exceptions | VIES returned an empty response. |
| [186](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/vies/ViesVatLookupService.java:186) | Fejl og exceptions | Could not parse VIES response. |

### web-server/src/main/java/com/bepa/eis/server/api/external/virk/cvr/CvrApiConfig.java

3 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [20](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/virk/cvr/CvrApiConfig.java:20) | Øvrige tekstkandidater | baseUri must not be null |
| [21](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/virk/cvr/CvrApiConfig.java:21) | Øvrige tekstkandidater | timeout must not be null |
| [22](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/virk/cvr/CvrApiConfig.java:22) | Øvrige tekstkandidater | userAgent must not be null |

### web-server/src/main/java/com/bepa/eis/server/api/external/virk/cvr/CvrapiDkLookupService.java

8 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [72](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/virk/cvr/CvrapiDkLookupService.java:72) | Fejl og exceptions | CVR API returned HTTP  |
| [79](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/virk/cvr/CvrapiDkLookupService.java:79) | Fejl og exceptions | CVR API returned no response data |
| [84](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/virk/cvr/CvrapiDkLookupService.java:84) | Fejl og exceptions | CVR API returned error status  |
| [96](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/virk/cvr/CvrapiDkLookupService.java:96) | Fejl og exceptions | CVR-nummer må ikke være null |
| [102](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/virk/cvr/CvrapiDkLookupService.java:102) | Fejl og exceptions | CVR-nummer skal bestå af præcis 8 cifre |
| [119](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/virk/cvr/CvrapiDkLookupService.java:119) | Fejl og exceptions | Fejl ved kald til CVR API |
| [122](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/virk/cvr/CvrapiDkLookupService.java:122) | Fejl og exceptions | CVR API kald blev afbrudt |
| [130](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/external/virk/cvr/CvrapiDkLookupService.java:130) | Fejl og exceptions | Kunne ikke parse svar fra CVR API |

### web-server/src/main/java/com/bepa/eis/server/api/generic/GenericDataProviderServlet.java

7 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [111](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericDataProviderServlet.java:111) | Fejl og exceptions | Invalid request :  |
| [155](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericDataProviderServlet.java:155) | Fejl og exceptions | Invalid request :  |
| [178](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericDataProviderServlet.java:178) | Fejl og exceptions | Error processing request |
| [217](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericDataProviderServlet.java:217) | Fejl og exceptions | Invalid requirement ID format |
| [231](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericDataProviderServlet.java:231) | Fejl og exceptions | Invalid version ID format |
| [252](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericDataProviderServlet.java:252) | Øvrige tekstkandidater | Error occurred during import. The error has been registered in the log. Please try again later. |
| [254](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericDataProviderServlet.java:254) | Øvrige tekstkandidater | Error occurred :  |

### web-server/src/main/java/com/bepa/eis/server/api/generic/GenericExporters.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [139](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericExporters.java:139) | Eksport og import | <?xml version=\"1.0\" encoding=\"UTF-8\"?> |
| [215](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericExporters.java:215) | Eksport og import | Exported rows:  |

### web-server/src/main/java/com/bepa/eis/server/api/generic/GenericImporters.java

11 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [66](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericImporters.java:66) | Fejl og exceptions | Import contains validation errors. Review the preview and fix the highlighted rows. |
| [230](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericImporters.java:230) | Fejl og exceptions | No import file was uploaded. |
| [244](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericImporters.java:244) | Fejl og exceptions | Unsupported import format:  |
| [249](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericImporters.java:249) | Fejl og exceptions | Import loader must return a List. |
| [339](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericImporters.java:339) | Eksport og import | ID is invalid. Use digits and dots only, up to five levels. |
| [346](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericImporters.java:346) | Eksport og import | Duplicate ID  |
| [357](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericImporters.java:357) | Eksport og import |  is required. |
| [387](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericImporters.java:387) | Eksport og import | ID is invalid. |
| [396](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericImporters.java:396) | Eksport og import | Missing parent ID  |
| [409](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericImporters.java:409) | Eksport og import | Missing ID  |
| [412](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericImporters.java:412) | Eksport og import | Missing ID  |

### web-server/src/main/java/com/bepa/eis/server/api/generic/GenericServlet.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [47](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericServlet.java:47) | Fejl og exceptions | Unable to create development session |
| [167](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/generic/GenericServlet.java:167) | Fejl og exceptions | Unable to redirect to the session-expired page |

### web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java

23 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [76](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:76) | Fejl og exceptions | Failed to lookup DataSource:  |
| [167](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:167) | Øvrige tekstkandidater | Missing username or password |
| [172](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:172) | Øvrige tekstkandidater | Login request was missing username or password |
| [178](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:178) | Øvrige tekstkandidater | Login request was missing username or password |
| [203](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:203) | Øvrige tekstkandidater | Invalid login attempt |
| [216](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:216) | Øvrige tekstkandidater | Account is locked |
| [217](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:217) | Øvrige tekstkandidater | User Account Locked - please contact your administrator |
| [218](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:218) | Øvrige tekstkandidater | Invalid username or password |
| [231](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:231) | Øvrige tekstkandidater | Could not load MFA state for user |
| [288](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:288) | Øvrige tekstkandidater | Login successful without MFA because MFA is globally disabled |
| [289](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:289) | Øvrige tekstkandidater | Login successful without MFA |
| [445](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:445) | Øvrige tekstkandidater | Login successful without MFA because MFA is globally disabled |
| [516](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:516) | Øvrige tekstkandidater | MFA setup started |
| [541](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:541) | Øvrige tekstkandidater | MFA verification required |
| [595](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:595) | Øvrige tekstkandidater | Invalid or expired MFA pre-auth session |
| [615](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:615) | Øvrige tekstkandidater | Too many MFA verification attempts |
| [636](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:636) | Øvrige tekstkandidater | MFA setup code could not be verified |
| [656](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:656) | Øvrige tekstkandidater | MFA setup code was verified, but MFA could not be enabled |
| [668](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:668) | Øvrige tekstkandidater | User enabled two-factor authentication |
| [679](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:679) | Øvrige tekstkandidater | MFA verification failed because MFA was not configured |
| [699](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:699) | Øvrige tekstkandidater | MFA verification code was invalid |
| [717](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:717) | Øvrige tekstkandidater | MFA verification successful |
| [738](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/LoginServlet.java:738) | Øvrige tekstkandidater | Login successful with MFA |

### web-server/src/main/java/com/bepa/eis/server/api/security/PasswordResetRequestServlet.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [22](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/PasswordResetRequestServlet.java:22) | Øvrige tekstkandidater | If an active account exists for this email address, you will receive a password reset link shortly. Check your inbox and spam folder. |
| [33](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/PasswordResetRequestServlet.java:33) | Øvrige tekstkandidater | Enter a valid email address. |
| [38](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/PasswordResetRequestServlet.java:38) | Øvrige tekstkandidater | Enter a valid email address. |
| [47](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/PasswordResetRequestServlet.java:47) | Øvrige tekstkandidater | Password reset is temporarily unavailable. Please try again later. |

### web-server/src/main/java/com/bepa/eis/server/api/security/PasswordResetServlet.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [56](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/PasswordResetServlet.java:56) | Øvrige tekstkandidater | Password must be at least 8 characters. |
| [61](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/PasswordResetServlet.java:61) | Øvrige tekstkandidater | Password must be no more than 128 characters. |
| [66](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/PasswordResetServlet.java:66) | Øvrige tekstkandidater | Passwords do not match. |
| [76](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/PasswordResetServlet.java:76) | Øvrige tekstkandidater | Password updated. |
| [76](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/PasswordResetServlet.java:76) | Øvrige tekstkandidater | Password reset failed. |
| [80](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/PasswordResetServlet.java:80) | Øvrige tekstkandidater | Password reset failed. Please request a new link or try again. |

### web-server/src/main/java/com/bepa/eis/server/api/security/RequestBaseUrl.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [20](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/RequestBaseUrl.java:20) | Fejl og exceptions | Invalid request origin |
| [25](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/RequestBaseUrl.java:25) | Fejl og exceptions | Invalid application context path |
| [35](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/RequestBaseUrl.java:35) | Fejl og exceptions | Invalid request host |
| [40](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/security/RequestBaseUrl.java:40) | Fejl og exceptions | Invalid request base URL |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AbstractAdminServlet.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [48](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AbstractAdminServlet.java:48) | Fejl og exceptions | Error processing request |
| [73](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AbstractAdminServlet.java:73) | Fejl og exceptions | Error processing request |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java

18 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [241](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:241) | Korte tekstkandidater | Success |
| [242](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:242) | Korte tekstkandidater | Failed |
| [337](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:337) | Korte tekstkandidater | Successful |
| [338](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:338) | Korte tekstkandidater | Failed |
| [482](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:482) | Korte tekstkandidater | Healthy |
| [483](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:483) | Korte tekstkandidater | Warning |
| [484](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:484) | Korte tekstkandidater | Critical |
| [618](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:618) | Øvrige tekstkandidater | Customer is suspended |
| [622](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:622) | Øvrige tekstkandidater | Customer is cancelled |
| [626](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:626) | Øvrige tekstkandidater | Payment is overdue |
| [630](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:630) | Øvrige tekstkandidater | Payment status:  |
| [634](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:634) | Øvrige tekstkandidater | Workflow requires manual attention |
| [637](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:637) | Øvrige tekstkandidater | Customer requires attention |
| [649](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:649) | Korte tekstkandidater | Critical |
| [653](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:653) | Korte tekstkandidater | Warning |
| [656](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:656) | Korte tekstkandidater | Warning |
| [781](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:781) | Korte tekstkandidater | Open |
| [884](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardCustomerProvider.java:884) | Korte tekstkandidater | Open |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardOverviewProvider.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [65](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardOverviewProvider.java:65) | Korte tekstkandidater | Critical |
| [67](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardOverviewProvider.java:67) | Korte tekstkandidater | Warning |
| [67](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardOverviewProvider.java:67) | Korte tekstkandidater | Critical |
| [67](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardOverviewProvider.java:67) | Korte tekstkandidater | Unknown |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java

61 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [84](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:84) | Øvrige tekstkandidater | Dashboard endpoint was not found. |
| [92](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:92) | Øvrige tekstkandidater | Dashboard data is unavailable. |
| [150](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:150) | Korte tekstkandidater | Healthy |
| [151](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:151) | Korte tekstkandidater | Warning |
| [152](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:152) | Korte tekstkandidater | Critical |
| [183](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:183) | Korte tekstkandidater | Success |
| [184](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:184) | Korte tekstkandidater | Failed |
| [208](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:208) | Korte tekstkandidater | Successful |
| [209](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:209) | Korte tekstkandidater | Failed |
| [337](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:337) | Korte tekstkandidater | Critical |
| [341](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:341) | Korte tekstkandidater | High |
| [345](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:345) | Korte tekstkandidater | Medium |
| [356](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:356) | Korte tekstkandidater | Critical |
| [357](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:357) | Korte tekstkandidater | High |
| [358](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:358) | Korte tekstkandidater | Medium |
| [361](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:361) | Øvrige tekstkandidater | Open today |
| [362](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:362) | Øvrige tekstkandidater | Resolved today |
| [455](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:455) | Korte tekstkandidater | Warning |
| [465](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:465) | Korte tekstkandidater | Success |
| [466](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:466) | Korte tekstkandidater | Failure |
| [469](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:469) | Øvrige tekstkandidater | Virk.dk / CVR |
| [470](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:470) | Øvrige tekstkandidater | Email Provider |
| [471](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:471) | Øvrige tekstkandidater | Payment Provider |
| [471](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:471) | Korte tekstkandidater | Warning |
| [472](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:472) | Øvrige tekstkandidater | SSO Provider |
| [473](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:473) | Øvrige tekstkandidater | External API Gateway |
| [476](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:476) | Øvrige tekstkandidater | Virk.dk / CVR |
| [477](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:477) | Øvrige tekstkandidater | Email Provider |
| [478](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:478) | Øvrige tekstkandidater | Payment Provider |
| [479](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:479) | Øvrige tekstkandidater | SSO Provider |
| [480](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:480) | Øvrige tekstkandidater | External API Gateway |
| [483](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:483) | Øvrige tekstkandidater | Virk.dk / CVR |
| [483](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:483) | Øvrige tekstkandidater | Company lookup |
| [483](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:483) | Øvrige tekstkandidater | 142 ms |
| [484](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:484) | Øvrige tekstkandidater | Payment Provider |
| [484](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:484) | Øvrige tekstkandidater | Payment validation |
| [484](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:484) | Øvrige tekstkandidater | 618 ms |
| [484](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:484) | Korte tekstkandidater | Warning |
| [485](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:485) | Øvrige tekstkandidater | Email Provider |
| [485](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:485) | Øvrige tekstkandidater | Send confirmation email |
| [485](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:485) | Øvrige tekstkandidater | 233 ms |
| [600](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:600) | Øvrige tekstkandidater | MFA enabled |
| [601](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:601) | Øvrige tekstkandidater | Missing MFA |
| [604](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:604) | Øvrige tekstkandidater | Failed login |
| [605](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:605) | Øvrige tekstkandidater | Permission change |
| [606](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:606) | Øvrige tekstkandidater | Locked account |
| [607](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:607) | Øvrige tekstkandidater | New admin |
| [610](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:610) | Øvrige tekstkandidater | Password policy |
| [611](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:611) | Øvrige tekstkandidater | MFA coverage |
| [611](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:611) | Korte tekstkandidater | Warning |
| [612](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:612) | Øvrige tekstkandidater | Admin users |
| [613](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:613) | Øvrige tekstkandidater | Suspicious activity |
| [613](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:613) | Korte tekstkandidater | Warning |
| [616](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:616) | Øvrige tekstkandidater | Changed user role |
| [616](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:616) | Øvrige tekstkandidater | user: peter@example.com |
| [617](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:617) | Øvrige tekstkandidater | Locked user account |
| [617](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:617) | Øvrige tekstkandidater | user: unknown@example.com |
| [617](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:617) | Korte tekstkandidater | Warning |
| [618](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:618) | Øvrige tekstkandidater | Created admin user |
| [618](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:618) | Øvrige tekstkandidater | user: new-admin@example.com |
| [618](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardServlet.java:618) | Korte tekstkandidater | Review |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardUserProvider.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [51](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardUserProvider.java:51) | Korte tekstkandidater | Successful |
| [52](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminDashboardUserProvider.java:52) | Korte tekstkandidater | Failed |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailStatusActionServlet.java

8 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [62](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailStatusActionServlet.java:62) | Øvrige tekstkandidater | action is required |
| [67](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailStatusActionServlet.java:67) | Øvrige tekstkandidater | Unsupported action:  |
| [77](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailStatusActionServlet.java:77) | Øvrige tekstkandidater | mailId is required |
| [85](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailStatusActionServlet.java:85) | Øvrige tekstkandidater | Mail was not found |
| [90](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailStatusActionServlet.java:90) | Øvrige tekstkandidater | Only failed mails can be resent |
| [107](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailStatusActionServlet.java:107) | Øvrige tekstkandidater | Recipient email is invalid |
| [124](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailStatusActionServlet.java:124) | Øvrige tekstkandidater | Mail could not be queued again |
| [128](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailStatusActionServlet.java:128) | Øvrige tekstkandidater | Mail queued again |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java

19 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [48](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:48) | Øvrige tekstkandidater | Template not found. |
| [51](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:51) | Fejl og exceptions | Could not load mail templates |
| [58](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:58) | Øvrige tekstkandidater | Reload the page before saving. |
| [61](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:61) | Øvrige tekstkandidater | The template file is too large. |
| [68](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:68) | Øvrige tekstkandidater | The template file is too large. |
| [75](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:75) | Øvrige tekstkandidater | Invalid import file. |
| [76](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:76) | Fejl og exceptions | Could not import mail templates |
| [81](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:81) | Øvrige tekstkandidater | Invalid template data. |
| [85](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:85) | Øvrige tekstkandidater | Enter a subject and email content. |
| [90](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:90) | Øvrige tekstkandidater | Template not found. |
| [101](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:101) | Fejl og exceptions | \r |
| [101](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:101) | Fejl og exceptions | \n |
| [105](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:105) | Øvrige tekstkandidater | Enter one valid recipient email address. |
| [109](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:109) | Øvrige tekstkandidater | The test email could not be queued. Please try again. |
| [110](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:110) | Øvrige tekstkandidater | Test email queued for  |
| [114](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:114) | Øvrige tekstkandidater | Unsupported template action. |
| [117](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:117) | Øvrige tekstkandidater | Someone else has changed this template. Close the dialog and reopen it. |
| [120](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:120) | Fejl og exceptions | Could not save mail template |
| [130](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminMailTemplateServlet.java:130) | Øvrige tekstkandidater | Only system administrators can access mail templates. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java

28 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [168](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:168) | Fejl og exceptions | User has no customer relation. |
| [172](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:172) | Fejl og exceptions | User has more than one customer relation. |
| [179](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:179) | Fejl og exceptions | User customer relation is missing customerId. |
| [210](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:210) | Øvrige tekstkandidater | User could not be saved. |
| [212](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:212) | Øvrige tekstkandidater | User created. Welcome email with password setup link queued. |
| [213](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:213) | Øvrige tekstkandidater | User created, but the welcome email could not be queued. Use Send reset link to retry. |
| [214](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:214) | Øvrige tekstkandidater | User saved. |
| [218](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:218) | Øvrige tekstkandidater | User could not be saved:  |
| [231](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:231) | Øvrige tekstkandidater | userId is required. |
| [241](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:241) | Øvrige tekstkandidater | Password reset link queued. |
| [241](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:241) | Øvrige tekstkandidater | Password reset link could not be queued. |
| [245](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:245) | Øvrige tekstkandidater | MFA was reset. |
| [245](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:245) | Øvrige tekstkandidater | MFA could not be reset. |
| [249](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:249) | Øvrige tekstkandidater | MFA was disabled. |
| [249](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:249) | Øvrige tekstkandidater | MFA could not be disabled. |
| [253](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:253) | Øvrige tekstkandidater | MFA reset flag was set. |
| [253](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:253) | Øvrige tekstkandidater | MFA reset flag could not be set. |
| [257](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:257) | Øvrige tekstkandidater | MFA reset flag was cleared. |
| [257](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:257) | Øvrige tekstkandidater | MFA reset flag could not be cleared. |
| [260](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:260) | Øvrige tekstkandidater | Unknown action:  |
| [331](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:331) | Korte tekstkandidater | Selected |
| [344](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:344) | Korte tekstkandidater | Default |
| [345](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:345) | Korte tekstkandidater | Required |
| [346](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:346) | Korte tekstkandidater | Disabled |
| [360](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:360) | Korte tekstkandidater | Light |
| [361](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:361) | Korte tekstkandidater | Blue |
| [362](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:362) | Korte tekstkandidater | Black |
| [454](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/AdminUserAdministrationServlet.java:454) | Korte tekstkandidater | Selected |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/MailTemplateTransfer.java

8 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [21](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/MailTemplateTransfer.java:21) | Fejl og exceptions | Invalid or unsupported mail template export file. |
| [24](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/MailTemplateTransfer.java:24) | Fejl og exceptions | Missing or duplicate template key. |
| [26](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/MailTemplateTransfer.java:26) | Fejl og exceptions | Unknown template:  |
| [32](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/MailTemplateTransfer.java:32) | Fejl og exceptions | Invalid content or metadata for  |
| [36](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/MailTemplateTransfer.java:36) | Fejl og exceptions | Missing, split or undeclared fields in  |
| [38](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/MailTemplateTransfer.java:38) | Fejl og exceptions | Invalid fields in  |
| [44](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/MailTemplateTransfer.java:44) | Fejl og exceptions | The import removes required fields from  |
| [49](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/MailTemplateTransfer.java:49) | Fejl og exceptions | Invalid template field metadata. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java

20 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [90](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:90) | Fejl og exceptions | Subscription editor import is not supported. |
| [116](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:116) | Fejl og exceptions | Subscription editor export is not supported. |
| [129](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:129) | Fejl og exceptions | Subscription plan data is required. |
| [139](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:139) | Fejl og exceptions | Invalid subscription module. |
| [143](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:143) | Fejl og exceptions | Valid from is required. |
| [147](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:147) | Fejl og exceptions | Valid to cannot be earlier than valid from. |
| [151](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:151) | Fejl og exceptions | The validity period overlaps with another subscription plan of the same type. |
| [177](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:177) | Fejl og exceptions | Subscription plan could not be saved. |
| [211](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:211) | Fejl og exceptions | Could not read generated subscription plan id. |
| [216](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:216) | Fejl og exceptions | SubscriptionPlanId is required for update. |
| [225](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:225) | Fejl og exceptions | Subscription plan not found. |
| [284](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:284) | Fejl og exceptions | Subscription plan data is required. |
| [299](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:299) | Korte tekstkandidater | Active |
| [315](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:315) | Korte tekstkandidater | Active |
| [440](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:440) | Korte tekstkandidater | Description |
| [445](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:445) | Korte tekstkandidater | Active |
| [487](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:487) | Korte tekstkandidater | Description |
| [490](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:490) | Korte tekstkandidater | Currency |
| [491](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:491) | Korte tekstkandidater | Active |
| [643](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorServlet.java:643) | Korte tekstkandidater | Standard |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorXmlDocument.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [45](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorXmlDocument.java:45) | Fejl og exceptions | Parent element is required. |
| [49](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorXmlDocument.java:49) | Fejl og exceptions | Element name is required. |
| [69](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorXmlDocument.java:69) | Fejl og exceptions | Parent element is required. |
| [73](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/admin/subscriptioneditor/SubscriptionEditorXmlDocument.java:73) | Fejl og exceptions | Source element is required. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java

21 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [27](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:27) | Fejl og exceptions | Customer is required |
| [716](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:716) | Øvrige tekstkandidater | Ascension Island |
| [718](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:718) | Øvrige tekstkandidater | Aland Islands |
| [719](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:719) | Øvrige tekstkandidater | Caribbean Netherlands |
| [720](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:720) | Øvrige tekstkandidater | Cyprus (CT) |
| [721](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:721) | Øvrige tekstkandidater | Western Sahara |
| [723](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:723) | Øvrige tekstkandidater | Isle of Man |
| [725](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:725) | Øvrige tekstkandidater | Saint Martin |
| [728](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:728) | Øvrige tekstkandidater | Saint Helena |
| [729](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:729) | Øvrige tekstkandidater | Svalbard and Jan Mayen |
| [730](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:730) | Øvrige tekstkandidater | Sint Maarten |
| [731](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:731) | Øvrige tekstkandidater | Tristan da Cunha |
| [732](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:732) | Øvrige tekstkandidater | French Southern Territories |
| [733](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:733) | Øvrige tekstkandidater | Universal service |
| [734](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:734) | Øvrige tekstkandidater | Vatican City |
| [748](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:748) | Fejl og exceptions | Failed to load cache:  |
| [755](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:755) | Fejl og exceptions | Failed to load cache:  |
| [766](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:766) | Fejl og exceptions | Failed to load cache:  |
| [773](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:773) | Fejl og exceptions | Failed to load cache:  |
| [802](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:802) | Fejl og exceptions | Ehcache cache alias not found:  |
| [802](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/CustomerLookupCache.java:802) | Fejl og exceptions |  (check ehcache.xml) |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/GenericLookup.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [53](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/GenericLookup.java:53) | Fejl og exceptions | Failed to lookup DataSource via JNDI name:  |
| [63](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/GenericLookup.java:63) | Fejl og exceptions | No sql defined  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/LookupCache.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [69](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/cache/LookupCache.java:69) | Fejl og exceptions | Could not load customer settings |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerAdministrationServlet.java

12 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [103](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerAdministrationServlet.java:103) | Svar og validering | customerId is required. |
| [147](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerAdministrationServlet.java:147) | Svar og validering | Customer could not be saved. |
| [155](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerAdministrationServlet.java:155) | Svar og validering | Subscription could not be saved. |
| [163](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerAdministrationServlet.java:163) | Svar og validering | Payment could not be saved. |
| [171](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerAdministrationServlet.java:171) | Svar og validering | Payment method could not be saved. |
| [185](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerAdministrationServlet.java:185) | Svar og validering | Customer module could not be saved. customerModuleId= |
| [190](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerAdministrationServlet.java:190) | Øvrige tekstkandidater | Customer administration data saved. |
| [192](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerAdministrationServlet.java:192) | Svar og validering | Customer administration data could not be saved:  |
| [472](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerAdministrationServlet.java:472) | Øvrige tekstkandidater | Customer onboarding |
| [481](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerAdministrationServlet.java:481) | Korte tekstkandidater | Optional |
| [485](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerAdministrationServlet.java:485) | Korte tekstkandidater | Required |
| [489](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerAdministrationServlet.java:489) | Korte tekstkandidater | Disabled |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerPaymentCallbackServlet.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [187](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerPaymentCallbackServlet.java:187) | Øvrige tekstkandidater | Payment callback received with status  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java

41 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [135](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:135) | Øvrige tekstkandidater | CVR number must contain exactly 8 digits. |
| [147](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:147) | Øvrige tekstkandidater | No company was found for the entered CVR number. |
| [161](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:161) | Øvrige tekstkandidater | Invalid CVR lookup request. |
| [167](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:167) | Øvrige tekstkandidater | Could not lookup company by CVR number. |
| [195](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:195) | Øvrige tekstkandidater | Invalid JSON request body. |
| [291](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:291) | Øvrige tekstkandidater | Request body is required. |
| [303](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:303) | Øvrige tekstkandidater | CVR number must contain exactly 8 digits when entered. |
| [307](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:307) | Øvrige tekstkandidater | Name is required. |
| [311](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:311) | Øvrige tekstkandidater | Address is required. |
| [315](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:315) | Øvrige tekstkandidater | Zip Code is required. |
| [319](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:319) | Øvrige tekstkandidater | City is required. |
| [323](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:323) | Øvrige tekstkandidater | Country is required. |
| [348](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:348) | Øvrige tekstkandidater | Phone is required. |
| [352](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:352) | Øvrige tekstkandidater | Contact email is required. |
| [356](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:356) | Øvrige tekstkandidater | Contact email must be valid. |
| [360](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:360) | Øvrige tekstkandidater | Payment information is required. |
| [370](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:370) | Øvrige tekstkandidater | Terms and conditions must be accepted. |
| [380](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:380) | Øvrige tekstkandidater | Subscription is required. |
| [384](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:384) | Øvrige tekstkandidater | Billing period is required. |
| [393](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:393) | Øvrige tekstkandidater | Selected billing period is not available for the chosen subscription. |
| [401](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:401) | Øvrige tekstkandidater | Cardholder name is required. |
| [407](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:407) | Øvrige tekstkandidater | Card number must contain between 13 and 19 digits. |
| [411](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:411) | Øvrige tekstkandidater | Expiry month is required. |
| [415](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:415) | Øvrige tekstkandidater | Expiry year is required. |
| [425](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:425) | Øvrige tekstkandidater | Expiry month and year must be numeric. |
| [429](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:429) | Øvrige tekstkandidater | Expiry month must be between 01 and 12. |
| [440](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:440) | Øvrige tekstkandidater | Payment card has expired. |
| [446](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:446) | Øvrige tekstkandidater | CVC must contain 3 or 4 digits. |
| [450](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:450) | Øvrige tekstkandidater | Billing zip code is required. |
| [499](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:499) | Øvrige tekstkandidater | Validation skipped for non-EU country. |
| [514](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:514) | Øvrige tekstkandidater | VIES validation is disabled. |
| [535](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:535) | Øvrige tekstkandidater | VAT number could not be validated with VIES. |
| [560](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:560) | Øvrige tekstkandidater | VAT number was validated successfully. |
| [574](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:574) | Øvrige tekstkandidater | Invalid VAT validation request. |
| [588](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:588) | Øvrige tekstkandidater | Could not validate VAT number through VIES. |
| [608](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:608) | Øvrige tekstkandidater | VAT number is required for EU countries. |
| [618](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:618) | Øvrige tekstkandidater | VAT number format is invalid. |
| [647](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:647) | Øvrige tekstkandidater | VAT number format is invalid. |
| [662](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:662) | Øvrige tekstkandidater | VAT number could not be validated with VIES. |
| [682](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:682) | Øvrige tekstkandidater | Invalid VAT validation request. |
| [691](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerRegistrationServlet.java:691) | Øvrige tekstkandidater | Could not validate VAT number through VIES. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowActionServlet.java

11 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [85](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowActionServlet.java:85) | Øvrige tekstkandidater | Action is missing. |
| [89](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowActionServlet.java:89) | Øvrige tekstkandidater | Token is missing. |
| [99](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowActionServlet.java:99) | Øvrige tekstkandidater | Unsupported action:  |
| [115](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowActionServlet.java:115) | Øvrige tekstkandidater | The request could not be completed |
| [118](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowActionServlet.java:118) | Øvrige tekstkandidater | The request could not be completed. |
| [133](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowActionServlet.java:133) | Øvrige tekstkandidater | Account confirmed |
| [134](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowActionServlet.java:134) | Øvrige tekstkandidater | Subscription continuation confirmed |
| [135](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowActionServlet.java:135) | Øvrige tekstkandidater | Subscription renewal confirmed |
| [136](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowActionServlet.java:136) | Øvrige tekstkandidater | Account reactivated |
| [137](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowActionServlet.java:137) | Øvrige tekstkandidater | Action completed |
| [150](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowActionServlet.java:150) | Indlejret HTML/XML |  ↵                 <!doctype html> ↵                 <html lang="en"> ↵                 <head> ↵                     <meta charset="UTF-8"> ↵                     <title>%s</title> ↵                     <style> ↵                         body { ↵                             margin: 0; ↵                             padding: 0; ↵                             background: #f4f6f8; ↵                             font-family: Arial, Helvetica, sans-serif; ↵                             color: #1f2933; ↵                         } ↵                         .page { ↵                             min-height: 100vh; ↵                             display: flex; ↵                             align-items: center; ↵                             justify-content: center; ↵                             padding: 24px; ↵                         } ↵                         .card { ↵                             width: 640px; ↵                             max-width: 100%%; ↵                             background: #ffffff; ↵                             border: 1px solid #d9e2ec; ↵                             border-radius: 8px; ↵                             overflow: hidden; ↵                             box-shadow: 0 10px 24px rgba(16, 42, 67, 0.08); ↵                         } ↵                         .header { ↵                             background: %s; ↵                             color: #ffffff; ↵                             padding: 24px 32px; ↵                         } ↵                         .header h1 { ↵                             margin: 0; ↵                             font-size: 22px; ↵                             line-height: 1.3; ↵                         } ↵                         .content { ↵                             padding: 32px; ↵                         } ↵                         .content p { ↵                             margin: 0 0 16px 0; ↵                             font-size: 16px; ↵                             line-height: 1.5; ↵                         } ↵                         .footer { ↵                             padding: 20px 32px; ↵                             background: #f8fafc; ↵                             border-top: 1px solid #d9e2ec; ↵                             color: #7b8794; ↵                             font-size: 12px; ↵                         } ↵                     </style> ↵                 </head> ↵                 <body> ↵                     <div class="page"> ↵                         <div class="card"> ↵                             <div class="header"> ↵                                 <h1>%s</h1> ↵                             </div> ↵                             <div class="content"> ↵                                 <p>%s</p> ↵                                 <p>You may now close this page.</p> ↵                             </div> ↵                             <div class="footer"> ↵                                 This page was generated automatically by BEPA EIS. ↵                             </div> ↵                         </div> ↵                     </div> ↵                 </body> ↵                 </html> ↵                  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java

20 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [78](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:78) | Øvrige tekstkandidater | action is required |
| [86](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:86) | Øvrige tekstkandidater | Unsupported action:  |
| [95](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:95) | Øvrige tekstkandidater | customerId is required |
| [99](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:99) | Øvrige tekstkandidater | Customer manually suspended. |
| [109](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:109) | Øvrige tekstkandidater | Customer could not be suspended |
| [140](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:140) | Øvrige tekstkandidater | Customer suspended |
| [148](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:148) | Øvrige tekstkandidater | customerId is required |
| [155](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:155) | Øvrige tekstkandidater | Customer could not be reactivated |
| [181](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:181) | Øvrige tekstkandidater | Customer manually reactivated. |
| [186](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:186) | Øvrige tekstkandidater | Customer reactivated |
| [194](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:194) | Øvrige tekstkandidater | workflowId is required |
| [201](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:201) | Øvrige tekstkandidater | Workflow was not found |
| [218](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:218) | Øvrige tekstkandidater | Workflow could not be retried |
| [227](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:227) | Øvrige tekstkandidater | Workflow retry requested. |
| [231](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:231) | Øvrige tekstkandidater | Workflow retry requested |
| [242](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:242) | Øvrige tekstkandidater | workflowId is required |
| [246](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:246) | Øvrige tekstkandidater | Workflow manually cancelled. |
| [253](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:253) | Øvrige tekstkandidater | Workflow was not found |
| [276](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:276) | Øvrige tekstkandidater | Workflow could not be cancelled |
| [289](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowAdminActionServlet.java:289) | Øvrige tekstkandidater | Workflow cancelled |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowStartServlet.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [54](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/customer/CustomerWorkflowStartServlet.java:54) | Øvrige tekstkandidater | Customer onboarding workflow started from API. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/enums/EntityRequestType.java

3 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [5](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/enums/EntityRequestType.java:5) | Enum-tekster | List of entities |
| [6](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/enums/EntityRequestType.java:6) | Enum-tekster | Edit single entity |
| [7](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/enums/EntityRequestType.java:7) | Enum-tekster | Create new entity |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/HelpServlet.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [24](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/HelpServlet.java:24) | Svar og validering | Invalid help page. |
| [33](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/HelpServlet.java:33) | Svar og validering | Help page not found. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java

20 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [107](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:107) | Svar og validering | cmd is required. |
| [143](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:143) | Fejl og exceptions | Menu export only supports XML. |
| [159](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:159) | Fejl og exceptions | Menu item was not found. |
| [169](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:169) | Fejl og exceptions | Invalid request :  |
| [185](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:185) | Fejl og exceptions | Import file is required. |
| [197](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:197) | Øvrige tekstkandidater | Import Menu Editor (matching IDs are updated) |
| [199](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:199) | Øvrige tekstkandidater | Import menus |
| [201](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:201) | Øvrige tekstkandidater | Menu item |
| [208](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:208) | Øvrige tekstkandidater | Menu items imported. |
| [210](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:210) | Fejl og exceptions | Import phase must be preview or execute. |
| [216](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:216) | Øvrige tekstkandidater | Menu item saved. |
| [221](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:221) | Fejl og exceptions | id is required. |
| [225](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:225) | Øvrige tekstkandidater | Menu item moved. |
| [225](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:225) | Øvrige tekstkandidater | Menu item is already at the edge. |
| [228](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:228) | Fejl og exceptions | Invalid request :  |
| [282](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:282) | Svar og validering | Project selection could not be saved. |
| [570](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:570) | Fejl og exceptions | Menu item data is required. |
| [597](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:597) | Fejl og exceptions | Import file must be a menu export XML file. |
| [608](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:608) | Fejl og exceptions | Invalid integer in  |
| [614](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/misc/MenuServlet.java:614) | Fejl og exceptions | Invalid boolean in  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettings.java

7 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [20](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettings.java:20) | Fejl og exceptions | Expected settings XML |
| [62](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettings.java:62) | Øvrige tekstkandidater | Unknown setting:  |
| [63](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettings.java:63) | Fejl og exceptions | project |
| [63](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettings.java:63) | Fejl og exceptions | Select a project first |
| [69](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettings.java:69) | Fejl og exceptions | Medium word maximum length must be at least the short word maximum length |
| [87](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettings.java:87) | Fejl og exceptions | Could not serialize settings |
| [101](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettings.java:101) | Fejl og exceptions | Could not parse settings XML |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettingsProvider.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [25](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettingsProvider.java:25) | Fejl og exceptions | Customer is required |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettingsServlet.java

5 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [25](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettingsServlet.java:25) | Øvrige tekstkandidater | Could not read customer settings |
| [25](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettingsServlet.java:25) | Øvrige tekstkandidater | Could not load settings |
| [37](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettingsServlet.java:37) | Øvrige tekstkandidater | The selected customer or project changed. Reload settings before saving. |
| [49](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettingsServlet.java:49) | Øvrige tekstkandidater | Could not save customer settings |
| [50](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/CustomerSettingsServlet.java:50) | Øvrige tekstkandidater | Could not save or reload settings. Reload the page to check the saved values. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java

19 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [9](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:9) | Skærmtekster og metadata | Customer MFA policy |
| [10](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:10) | Skærmtekster og metadata | OPTIONAL allows users to choose MFA. REQUIRED requires MFA for every user. DISABLED disables MFA. |
| [11](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:11) | Skærmtekster og metadata | Enable fuzzy matching |
| [12](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:12) | Skærmtekster og metadata | Allow similar spellings to match, for example system and sytem. Short words still require an exact match. |
| [13](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:13) | Skærmtekster og metadata | Minimum matching words |
| [14](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:14) | Skærmtekster og metadata | Number of distinct matching words required for a match. Repeating the same word does not increase the count. |
| [15](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:15) | Skærmtekster og metadata | Short word maximum length |
| [16](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:16) | Skærmtekster og metadata | Words up to this length must match exactly. With 4, coal and cool do not match. |
| [17](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:17) | Skærmtekster og metadata | Medium word maximum length |
| [18](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:18) | Skærmtekster og metadata | Words longer than the short limit and up to this length use the medium edit limit. Longer words use the long edit limit. |
| [19](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:19) | Skærmtekster og metadata | Medium word maximum edits |
| [20](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:20) | Skærmtekster og metadata | Maximum character insertions, deletions or replacements for medium words. One edit allows system to match sytem. |
| [21](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:21) | Skærmtekster og metadata | Long word maximum edits |
| [22](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:22) | Skærmtekster og metadata | Maximum character insertions, deletions or replacements for words longer than the medium limit. Zero requires exact spelling. |
| [23](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:23) | Skærmtekster og metadata | Stopwords |
| [25](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:25) | Skærmtekster og metadata | Comma-separated words excluded before matching, for example og,the,and. Leave empty to disable stopword filtering. |
| [29](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:29) | Fejl og exceptions |  is required |
| [31](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:31) | Fejl og exceptions |  is too long |
| [43](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/settings/SettingDefinition.java:43) | Fejl og exceptions | Invalid value for  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/PageViewer.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [53](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/PageViewer.java:53) | Fejl og exceptions | Page not found :  |
| [167](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/PageViewer.java:167) | Fejl og exceptions | HTML resource not found:  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/SessionExpiredServlet.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [53](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/SessionExpiredServlet.java:53) | Fejl og exceptions | HTML resource not found:  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceProvider.java

10 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [92](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceProvider.java:92) | Fejl og exceptions | Department is required. |
| [140](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceProvider.java:140) | Fejl og exceptions | Department was saved but could not be reloaded. |
| [165](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceProvider.java:165) | Fejl og exceptions | Insert department failed. |
| [175](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceProvider.java:175) | Fejl og exceptions | Could not read generated department id. |
| [187](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceProvider.java:187) | Fejl og exceptions | DepartmentId is required for update. |
| [199](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceProvider.java:199) | Fejl og exceptions | Department not found for current customer. |
| [218](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceProvider.java:218) | Fejl og exceptions | DepartmentName is required. |
| [222](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceProvider.java:222) | Fejl og exceptions | DepartmentName must be maximum 10 characters. |
| [226](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceProvider.java:226) | Fejl og exceptions | DepartmentDescription must be maximum 255 characters. |
| [232](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceProvider.java:232) | Fejl og exceptions | CustomerId is missing from the current session. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceServlet.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [26](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceServlet.java:26) | Fejl og exceptions | Department import is not supported. |
| [39](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceServlet.java:39) | Fejl og exceptions | Department data is required. |
| [67](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceServlet.java:67) | Fejl og exceptions | Department was not found. |
| [86](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceServlet.java:86) | Fejl og exceptions | Department export is not supported. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceXmlDocument.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [45](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceXmlDocument.java:45) | Fejl og exceptions | Parent element is required. |
| [49](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceXmlDocument.java:49) | Fejl og exceptions | Element name is required. |
| [69](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceXmlDocument.java:69) | Fejl og exceptions | Parent element is required. |
| [73](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/department/DepartmentMaintenanceXmlDocument.java:73) | Fejl og exceptions | Source element is required. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/incidents/IncidentMaintenanceServlet.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [30](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/incidents/IncidentMaintenanceServlet.java:30) | Fejl og exceptions | Incident import is not supported. |
| [35](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/incidents/IncidentMaintenanceServlet.java:35) | Fejl og exceptions | Incident save is not supported. |
| [45](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/incidents/IncidentMaintenanceServlet.java:45) | Fejl og exceptions | Incident edit is not supported. |
| [50](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/incidents/IncidentMaintenanceServlet.java:50) | Fejl og exceptions | Incident create is not supported. |
| [55](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/incidents/IncidentMaintenanceServlet.java:55) | Fejl og exceptions | Incident export is not supported. |
| [78](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/incidents/IncidentMaintenanceServlet.java:78) | Fejl og exceptions | Could not load incidents. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/incidents/IncidentMaintenanceXmlDocument.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [45](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/incidents/IncidentMaintenanceXmlDocument.java:45) | Fejl og exceptions | Parent element is required. |
| [49](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/incidents/IncidentMaintenanceXmlDocument.java:49) | Fejl og exceptions | Element name is required. |
| [69](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/incidents/IncidentMaintenanceXmlDocument.java:69) | Fejl og exceptions | Parent element is required. |
| [73](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/admin/incidents/IncidentMaintenanceXmlDocument.java:73) | Fejl og exceptions | Source element is required. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java

15 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [184](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:184) | Fejl og exceptions | A baseline with this tag-name already exists for the selected customer and project. |
| [231](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:231) | Fejl og exceptions | Baseline was not found. |
| [239](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:239) | Fejl og exceptions | A baseline with this tag-name already exists for the selected customer and project. |
| [251](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:251) | Fejl og exceptions | Baseline was not found. |
| [316](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:316) | Fejl og exceptions | Error getting entity records for baseline:  |
| [333](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:333) | Korte tekstkandidater | Created |
| [335](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:335) | Korte tekstkandidater | Modified |
| [335](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:335) | Korte tekstkandidater | Inactivated |
| [393](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:393) | Fejl og exceptions | WebSession is required. |
| [397](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:397) | Fejl og exceptions | CustomerId is required. |
| [401](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:401) | Fejl og exceptions | ProjectId is required. |
| [405](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:405) | Fejl og exceptions | ChangedByUserId is required. |
| [414](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:414) | Fejl og exceptions | Tag-name is required. |
| [418](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:418) | Fejl og exceptions | Tag-name must be maximum 150 characters. |
| [422](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineProvider.java:422) | Fejl og exceptions | Description is required. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineServlet.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [35](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineServlet.java:35) | Fejl og exceptions | Baseline import is not supported. |
| [45](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineServlet.java:45) | Fejl og exceptions | Only the project owner can create baselines. |
| [59](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineServlet.java:59) | Fejl og exceptions | Baseline data is required. |
| [164](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineServlet.java:164) | Fejl og exceptions | Baseline was not found. |
| [276](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineServlet.java:276) | Fejl og exceptions | Baseline export is not supported. |
| [285](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineServlet.java:285) | Fejl og exceptions | Baseline overview is not supported. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineXmlDocument.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [58](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineXmlDocument.java:58) | Fejl og exceptions | Parent element is required. |
| [62](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineXmlDocument.java:62) | Fejl og exceptions | Element name is required. |
| [93](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineXmlDocument.java:93) | Fejl og exceptions | Parent element is required. |
| [97](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/baseline/BaselineXmlDocument.java:97) | Fejl og exceptions | Source element is required. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java

17 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [187](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:187) | Fejl og exceptions | Lookup row is required. |
| [233](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:233) | Fejl og exceptions | Lookup import data is required. |
| [249](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:249) | Fejl og exceptions | LookupTypeId is required for import. |
| [268](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:268) | Fejl og exceptions | LookupId is required for import. |
| [335](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:335) | Fejl og exceptions | Insert lookup row failed. |
| [345](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:345) | Fejl og exceptions | Could not read generated lookup id. |
| [394](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:394) | Fejl og exceptions | LookupId is required for update. |
| [408](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:408) | Fejl og exceptions | Lookup row was not updated. |
| [417](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:417) | Fejl og exceptions | LookupType is required. |
| [421](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:421) | Fejl og exceptions | Unknown lookup type:  |
| [438](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:438) | Fejl og exceptions | LookupCode must be unique within the selected lookup type. |
| [486](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:486) | Fejl og exceptions | LookupType is required. |
| [490](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:490) | Fejl og exceptions | LookupCode is required. |
| [494](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:494) | Fejl og exceptions | LookupCode must be maximum 50 characters. |
| [498](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:498) | Fejl og exceptions | LookupDescription must be maximum 255 characters. |
| [502](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:502) | Fejl og exceptions | Color must be maximum 20 characters. |
| [517](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceProvider.java:517) | Fejl og exceptions | Color must be a browser-supported color name or hex value. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceXmlDocument.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [45](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceXmlDocument.java:45) | Fejl og exceptions | Parent element is required. |
| [49](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceXmlDocument.java:49) | Fejl og exceptions | Element name is required. |
| [69](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceXmlDocument.java:69) | Fejl og exceptions | Parent element is required. |
| [73](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupMaintenanceXmlDocument.java:73) | Fejl og exceptions | Source element is required. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupServlet.java

8 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [45](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupServlet.java:45) | Fejl og exceptions | No lookup rows imported. |
| [59](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupServlet.java:59) | Fejl og exceptions | Lookup data is required. |
| [90](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupServlet.java:90) | Fejl og exceptions | Lookup row was not found. |
| [118](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupServlet.java:118) | Fejl og exceptions | Lookup export only supports XML. |
| [128](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupServlet.java:128) | Øvrige tekstkandidater | Lookup Administration |
| [141](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupServlet.java:141) | Fejl og exceptions | Lookup overview is not supported. |
| [244](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupServlet.java:244) | Fejl og exceptions | Import file is required. |
| [258](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookup/LookupServlet.java:258) | Fejl og exceptions | Import file must be a lookup export XML file. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java

40 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [147](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:147) | Fejl og exceptions | Lookup row is required. |
| [155](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:155) | Fejl og exceptions | LookupType is required. |
| [193](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:193) | Fejl og exceptions | Classification rows must be saved through saveClassificationRow. |
| [215](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:215) | Fejl og exceptions | Classification rows must be saved through saveClassificationRow. |
| [223](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:223) | Fejl og exceptions | Lookup row was not found for current customer and project. |
| [229](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:229) | Fejl og exceptions | Unsupported lookup type. |
| [250](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:250) | Fejl og exceptions | Cannot deactivate readiness level  |
| [251](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:251) | Fejl og exceptions |  and higher: TRL  |
| [251](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:251) | Fejl og exceptions |  or higher is used by a latest system version in the current project (including inactive systems). |
| [267](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:267) | Fejl og exceptions | Cannot deactivate readiness levels: an affected IRL level is used by a latest interface version in the current project (all entity types, including inactive entities). |
| [292](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:292) | Fejl og exceptions | TRL row id is required. |
| [304](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:304) | Fejl og exceptions | IRL row id is required. |
| [316](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:316) | Fejl og exceptions | SRL row id is required. |
| [328](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:328) | Fejl og exceptions | Classification row is required. |
| [385](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:385) | Fejl og exceptions | A classification with code ' |
| [386](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:386) | Fejl og exceptions | ' already exists for the current project. |
| [399](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:399) | Fejl og exceptions | Classification row id is required. |
| [418](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:418) | Fejl og exceptions | Classification row was not found for current customer and project. |
| [430](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:430) | Fejl og exceptions | Could not determine the next ClassId. |
| [520](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:520) | Fejl og exceptions | Lookup row was not found for current customer and project. |
| [651](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:651) | Fejl og exceptions | LookupId is required. |
| [655](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:655) | Fejl og exceptions | LookupLevel is required. |
| [659](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:659) | Fejl og exceptions | IRLCode is required. |
| [663](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:663) | Fejl og exceptions | Name is required. |
| [667](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:667) | Fejl og exceptions | Description is required. |
| [673](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:673) | Fejl og exceptions | TRL name must be maximum 75 characters. |
| [676](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:676) | Fejl og exceptions | TRL description must be maximum 4000 characters. |
| [681](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:681) | Fejl og exceptions | IRL name must be maximum 75 characters. |
| [684](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:684) | Fejl og exceptions | IRL description must be maximum 255 characters. |
| [689](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:689) | Fejl og exceptions | SRL name must be maximum 255 characters. |
| [692](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:692) | Fejl og exceptions | SRL description must be maximum 255 characters. |
| [698](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:698) | Fejl og exceptions | Color must be maximum 20 characters. |
| [710](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:710) | Fejl og exceptions | Code is required. |
| [714](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:714) | Fejl og exceptions | Description is required. |
| [718](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:718) | Fejl og exceptions | Code must start with A-Z and may be followed by A-Z, 0-9 or _. |
| [722](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:722) | Fejl og exceptions | Description must be maximum 255 characters. |
| [729](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:729) | Fejl og exceptions | CustomerId is missing from the current session. |
| [737](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:737) | Fejl og exceptions | ProjectId is missing from the current session. |
| [763](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:763) | Fejl og exceptions | IRLCode is required. |
| [778](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceProvider.java:778) | Fejl og exceptions | Unsupported IRLCode:  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceServlet.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [23](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceServlet.java:23) | Fejl og exceptions | Lookup maintenance import is not supported. |
| [36](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceServlet.java:36) | Fejl og exceptions | Lookup row data is required. |
| [79](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceServlet.java:79) | Fejl og exceptions | Lookup maintenance edit endpoint is not supported. |
| [84](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceServlet.java:84) | Fejl og exceptions | Lookup maintenance create endpoint is not supported. |
| [89](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceServlet.java:89) | Fejl og exceptions | Lookup maintenance export is not supported. |
| [207](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceServlet.java:207) | Fejl og exceptions | LookupType is required. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceXmlDocument.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [45](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceXmlDocument.java:45) | Fejl og exceptions | Parent element is required. |
| [49](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceXmlDocument.java:49) | Fejl og exceptions | Element name is required. |
| [69](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceXmlDocument.java:69) | Fejl og exceptions | Parent element is required. |
| [73](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/lookupmaintenance/LookupValueMaintenanceXmlDocument.java:73) | Fejl og exceptions | Source element is required. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/relationdiagram/RelationDiagramServlet.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [27](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/relationdiagram/RelationDiagramServlet.java:27) | Fejl og exceptions | Invalid import request |
| [32](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/relationdiagram/RelationDiagramServlet.java:32) | Fejl og exceptions | Invalid save request |
| [37](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/relationdiagram/RelationDiagramServlet.java:37) | Fejl og exceptions | Invalid list request |
| [42](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/relationdiagram/RelationDiagramServlet.java:42) | Fejl og exceptions | Invalid edit request |
| [47](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/relationdiagram/RelationDiagramServlet.java:47) | Fejl og exceptions | Invalid create request |
| [59](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/relationdiagram/RelationDiagramServlet.java:59) | Fejl og exceptions | Invalid export request |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderExporters.java

9 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [10](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderExporters.java:10) | Eksport og import | ID |
| [11](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderExporters.java:11) | Eksport og import | Level |
| [12](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderExporters.java:12) | Eksport og import | Name |
| [13](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderExporters.java:13) | Eksport og import | Description |
| [14](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderExporters.java:14) | Eksport og import | ChangedBy |
| [15](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderExporters.java:15) | Eksport og import | Changed |
| [16](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderExporters.java:16) | Eksport og import | Active |
| [44](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderExporters.java:44) | Eksport og import | Stakeholder Requirements |
| [59](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderExporters.java:59) | Eksport og import | Stakeholder Requirements Export |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderImporters.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [78](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderImporters.java:78) | Eksport og import | Import entities |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderInfo.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [46](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderInfo.java:46) | Fejl og exceptions | Invalid entity request type |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderServlet.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [44](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderServlet.java:44) | Fejl og exceptions | No entities imported |
| [79](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderServlet.java:79) | Fejl og exceptions | Failed to parse stakeholder XML |
| [100](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderServlet.java:100) | Fejl og exceptions | Invalid overview request |
| [191](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholder/StakeholderServlet.java:191) | Fejl og exceptions | Unsupported export format:  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementExporters.java

9 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [9](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementExporters.java:9) | Eksport og import | ID |
| [10](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementExporters.java:10) | Eksport og import | Level |
| [11](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementExporters.java:11) | Eksport og import | Name |
| [12](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementExporters.java:12) | Eksport og import | Description |
| [13](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementExporters.java:13) | Eksport og import | ChangedBy |
| [14](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementExporters.java:14) | Eksport og import | Changed |
| [15](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementExporters.java:15) | Eksport og import | Active |
| [43](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementExporters.java:43) | Eksport og import | Stakeholder Requirements |
| [58](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementExporters.java:58) | Eksport og import | Stakeholder Requirements Export |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementImporters.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [79](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementImporters.java:79) | Eksport og import | Import Stakeholder Requirements |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementInfo.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [46](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementInfo.java:46) | Fejl og exceptions | Invalid entity request type |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementServlet.java

8 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [53](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementServlet.java:53) | Fejl og exceptions | No entities imported |
| [91](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementServlet.java:91) | Fejl og exceptions | Failed to parse stakeholder requirement XML |
| [112](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementServlet.java:112) | Fejl og exceptions | Invalid overview request |
| [199](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementServlet.java:199) | Øvrige tekstkandidater | Stakeholder Requirement |
| [204](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementServlet.java:204) | Øvrige tekstkandidater | Stakeholder Requirement |
| [209](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementServlet.java:209) | Øvrige tekstkandidater | Stakeholder Requirement |
| [214](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementServlet.java:214) | Øvrige tekstkandidater | Stakeholder Requirement |
| [216](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/stakeholderrequirement/StakeholderRequirementServlet.java:216) | Fejl og exceptions | Unsupported export format:  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java

15 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [9](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:9) | Eksport og import | ID |
| [10](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:10) | Eksport og import | Level |
| [11](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:11) | Eksport og import | Name |
| [12](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:12) | Eksport og import | Description |
| [13](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:13) | Eksport og import | VerificationStatus |
| [14](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:14) | Eksport og import | RationalStatement |
| [15](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:15) | Eksport og import | RequirementCaptured |
| [16](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:16) | Eksport og import | Status |
| [17](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:17) | Eksport og import | BusinessPriority |
| [18](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:18) | Eksport og import | Owner |
| [19](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:19) | Eksport og import | ChangedBy |
| [20](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:20) | Eksport og import | Changed |
| [21](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:21) | Eksport og import | Active |
| [55](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:55) | Eksport og import | Systems Requirements |
| [70](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementExporters.java:70) | Eksport og import | Systems Requirements Export |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementImporters.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [79](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementImporters.java:79) | Eksport og import | Import Systems Requirements |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementInfo.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [44](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementInfo.java:44) | Fejl og exceptions | Invalid entity request type |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementServlet.java

9 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [50](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementServlet.java:50) | Fejl og exceptions | No entities imported |
| [79](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementServlet.java:79) | Øvrige tekstkandidater | ' has been  |
| [88](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementServlet.java:88) | Fejl og exceptions | Failed to parse system requirement XML |
| [109](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementServlet.java:109) | Fejl og exceptions | Invalid overview request |
| [219](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementServlet.java:219) | Øvrige tekstkandidater | Systems Requirement |
| [224](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementServlet.java:224) | Øvrige tekstkandidater | Systems Requirement |
| [229](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementServlet.java:229) | Øvrige tekstkandidater | Systems Requirement |
| [234](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementServlet.java:234) | Øvrige tekstkandidater | Systems Requirement |
| [236](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemrequirement/SystemRequirementServlet.java:236) | Fejl og exceptions | Unsupported export format:  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java

13 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [9](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java:9) | Eksport og import | SBS |
| [10](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java:10) | Eksport og import | Level |
| [11](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java:11) | Eksport og import | Name |
| [12](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java:12) | Eksport og import | SystemOwner |
| [13](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java:13) | Eksport og import | Department |
| [14](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java:14) | Eksport og import | TRL |
| [15](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java:15) | Eksport og import | DeadlineNextTRL |
| [16](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java:16) | Eksport og import | DeadlineFinalized |
| [17](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java:17) | Eksport og import | ChangedBy |
| [18](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java:18) | Eksport og import | Changed |
| [19](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java:19) | Eksport og import | Active |
| [51](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java:51) | Eksport og import | Physical Architecture |
| [66](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownExporters.java:66) | Eksport og import | Physical Architecture Export |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownImporters.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [91](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownImporters.java:91) | Eksport og import | Import Physical Structures |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownInfo.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [45](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownInfo.java:45) | Fejl og exceptions | Invalid entity request type |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownServlet.java

9 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [54](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownServlet.java:54) | Fejl og exceptions | No entities imported |
| [83](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownServlet.java:83) | Øvrige tekstkandidater | ' has been  |
| [92](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownServlet.java:92) | Fejl og exceptions | Failed to parse p-sys XML |
| [114](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownServlet.java:114) | Fejl og exceptions | Invalid overview request |
| [216](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownServlet.java:216) | Øvrige tekstkandidater | Systems Breakdown |
| [221](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownServlet.java:221) | Øvrige tekstkandidater | Systems Breakdown |
| [226](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownServlet.java:226) | Øvrige tekstkandidater | Systems Breakdown |
| [231](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownServlet.java:231) | Øvrige tekstkandidater | Systems Breakdown |
| [233](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/systemsbreakdown/SystemBreakdownServlet.java:233) | Fejl og exceptions | Unsupported export format:  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/MatrixColumnsData.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [21](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/MatrixColumnsData.java:21) | Korte tekstkandidater | Unknown |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/MatrixRowsData.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [17](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/MatrixRowsData.java:17) | Korte tekstkandidater | Unknown |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/MatrixStylesData.java

5 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [14](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/MatrixStylesData.java:14) | Korte tekstkandidater | Normal |
| [15](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/MatrixStylesData.java:15) | Øvrige tekstkandidater | No relations |
| [16](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/MatrixStylesData.java:16) | Øvrige tekstkandidater | Possible relations |
| [17](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/MatrixStylesData.java:17) | Øvrige tekstkandidater | Confirmed relations |
| [18](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/MatrixStylesData.java:18) | Øvrige tekstkandidater | Not relevant |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/TraceabilityMatrixDocument.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [174](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/TraceabilityMatrixDocument.java:174) | Øvrige tekstkandidater | Traceability Matrix |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/TraceabilityMatrixServlet.java

7 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [119](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/TraceabilityMatrixServlet.java:119) | Fejl og exceptions | Invalid import request |
| [124](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/TraceabilityMatrixServlet.java:124) | Fejl og exceptions | Invalid save request |
| [129](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/TraceabilityMatrixServlet.java:129) | Fejl og exceptions | Invalid list request |
| [134](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/TraceabilityMatrixServlet.java:134) | Fejl og exceptions | Invalid edit request |
| [139](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/TraceabilityMatrixServlet.java:139) | Fejl og exceptions | Invalid create request |
| [319](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/TraceabilityMatrixServlet.java:319) | Fejl og exceptions | Missing required parameter:  |
| [368](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/basis/traceability/TraceabilityMatrixServlet.java:368) | Fejl og exceptions | Unable to write error response |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityAttachmentProvider.java

3 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [151](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityAttachmentProvider.java:151) | Fejl og exceptions | Insert entity attachment failed for entityId =  |
| [172](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityAttachmentProvider.java:172) | Fejl og exceptions | Insert blob failed |
| [182](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityAttachmentProvider.java:182) | Fejl og exceptions | Insert blob failed: no generated key returned |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityHistorySnapshot.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [33](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityHistorySnapshot.java:33) | Øvrige tekstkandidater | (CASE WHEN  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityLinkProvider.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [114](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityLinkProvider.java:114) | Fejl og exceptions | Insert entity link failed for entityId= |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityNoteProvider.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [108](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityNoteProvider.java:108) | Fejl og exceptions | Insert entity note failed for entityId= |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java

18 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [103](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:103) | Øvrige tekstkandidater | Stakeholder requirements |
| [104](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:104) | Øvrige tekstkandidater | System requirement |
| [106](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:106) | Øvrige tekstkandidater | Logical Architecture |
| [107](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:107) | Øvrige tekstkandidater | Physical Architecture |
| [164](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:164) | Øvrige tekstkandidater | A relation already exists between :\n\n |
| [167](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:167) | Øvrige tekstkandidater | To  :  |
| [177](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:177) | Øvrige tekstkandidater | Not allowed to create a relation from ' |
| [177](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:177) | Øvrige tekstkandidater | ' to ' |
| [196](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:196) | Øvrige tekstkandidater | Unknown relation type:  |
| [203](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:203) | Øvrige tekstkandidater | The relation type ' |
| [203](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:203) | Øvrige tekstkandidater | ' is not allowed for this entity pair. |
| [224](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:224) | Fejl og exceptions | You cannot create a relation from an entity to itself. |
| [408](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:408) | Øvrige tekstkandidater | Unknown error |
| [412](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:412) | Svar og validering | Unknown error |
| [421](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:421) | Fejl og exceptions | Invalid integer parameter:  |
| [436](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:436) | Fejl og exceptions | Missing required parameter:  |
| [460](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:460) | Fejl og exceptions | Missing relation list request root element. |
| [913](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationListServlet.java:913) | Fejl og exceptions | Missing entity type for relation list request. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationProvider.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [225](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/EntityRelationProvider.java:225) | Fejl og exceptions | EntityRelationProvider: currEntityId != rs.getInt(EntityId.FIELD_NAME) |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/MatrixXlsxExporter.java

3 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [97](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/MatrixXlsxExporter.java:97) | Fejl og exceptions | Invalid matrix color. |
| [107](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/MatrixXlsxExporter.java:107) | Fejl og exceptions | Invalid or oversized matrix. |
| [115](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/common/MatrixXlsxExporter.java:115) | Fejl og exceptions | Invalid matrix cell. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/master/srlcalculation/SrlCalculationDocument.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [254](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/master/srlcalculation/SrlCalculationDocument.java:254) | Fejl og exceptions | Cannot normalise interface cells: maximum active IRL ID must exceed 2 |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/master/srlcalculation/SrlCalculationServlet.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [22](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/master/srlcalculation/SrlCalculationServlet.java:22) | Fejl og exceptions | Invalid import request |
| [27](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/master/srlcalculation/SrlCalculationServlet.java:27) | Fejl og exceptions | Invalid save request |
| [33](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/master/srlcalculation/SrlCalculationServlet.java:33) | Fejl og exceptions | Invalid list request |
| [39](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/master/srlcalculation/SrlCalculationServlet.java:39) | Fejl og exceptions | Invalid edit request |
| [45](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/master/srlcalculation/SrlCalculationServlet.java:45) | Fejl og exceptions | Invalid create request |
| [50](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/master/srlcalculation/SrlCalculationServlet.java:50) | Fejl og exceptions | Invalid export request |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/irlmonitor/DashboardIrlDocument.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [145](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/irlmonitor/DashboardIrlDocument.java:145) | Korte tekstkandidater | Overdue |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/irlmonitor/DashboardIrlServlet.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [27](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/irlmonitor/DashboardIrlServlet.java:27) | Fejl og exceptions | Invalid import request |
| [32](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/irlmonitor/DashboardIrlServlet.java:32) | Fejl og exceptions | Invalid save request |
| [37](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/irlmonitor/DashboardIrlServlet.java:37) | Fejl og exceptions | Invalid list request |
| [42](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/irlmonitor/DashboardIrlServlet.java:42) | Fejl og exceptions | Invalid edit request |
| [47](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/irlmonitor/DashboardIrlServlet.java:47) | Fejl og exceptions | Invalid create request |
| [59](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/irlmonitor/DashboardIrlServlet.java:59) | Fejl og exceptions | Invalid export request |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/systemsteamwork/DashboardSystemsTeamworkDocument.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [212](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/systemsteamwork/DashboardSystemsTeamworkDocument.java:212) | Korte tekstkandidater | Overdue |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/systemsteamwork/DashboardSystemsTeamworkServlet.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [27](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/systemsteamwork/DashboardSystemsTeamworkServlet.java:27) | Fejl og exceptions | Invalid import request |
| [32](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/systemsteamwork/DashboardSystemsTeamworkServlet.java:32) | Fejl og exceptions | Invalid save request |
| [37](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/systemsteamwork/DashboardSystemsTeamworkServlet.java:37) | Fejl og exceptions | Invalid list request |
| [42](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/systemsteamwork/DashboardSystemsTeamworkServlet.java:42) | Fejl og exceptions | Invalid edit request |
| [47](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/systemsteamwork/DashboardSystemsTeamworkServlet.java:47) | Fejl og exceptions | Invalid create request |
| [59](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/dashboard/systemsteamwork/DashboardSystemsTeamworkServlet.java:59) | Fejl og exceptions | Invalid export request |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureExporters.java

9 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [10](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureExporters.java:10) | Eksport og import | ID |
| [11](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureExporters.java:11) | Eksport og import | Level |
| [12](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureExporters.java:12) | Eksport og import | Name |
| [13](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureExporters.java:13) | Eksport og import | Description |
| [25](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureExporters.java:25) | Eksport og import | ChangedBy |
| [26](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureExporters.java:26) | Eksport og import | Changed |
| [27](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureExporters.java:27) | Eksport og import | Active |
| [87](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureExporters.java:87) | Eksport og import | Functional Architecture |
| [102](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureExporters.java:102) | Eksport og import | Functional Architecture Export |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureImporters.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [92](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureImporters.java:92) | Eksport og import | Import Functional Structures |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureInfo.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [46](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureInfo.java:46) | Fejl og exceptions | Invalid entity request type |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureServlet.java

8 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [51](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureServlet.java:51) | Fejl og exceptions | No entities imported |
| [95](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureServlet.java:95) | Fejl og exceptions | Failed to parse functional structure XML |
| [116](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureServlet.java:116) | Fejl og exceptions | Invalid overview request |
| [220](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureServlet.java:220) | Øvrige tekstkandidater | Functional Architecture |
| [225](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureServlet.java:225) | Øvrige tekstkandidater | Functional Architecture |
| [230](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureServlet.java:230) | Øvrige tekstkandidater | Functional Architecture |
| [235](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureServlet.java:235) | Øvrige tekstkandidater | Functional Architecture |
| [237](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/functionalstructure/FunctionalStructureServlet.java:237) | Fejl og exceptions | Unsupported export format:  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/FsysInterfaceMatrixDocument.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [32](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/FsysInterfaceMatrixDocument.java:32) | Øvrige tekstkandidater | Interface Management |
| [37](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/FsysInterfaceMatrixDocument.java:37) | Øvrige tekstkandidater | To Functional Architecture |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixEntityOrder.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [32](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixEntityOrder.java:32) | Fejl og exceptions | Unsupported interface entity type:  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixProvider.java

10 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [189](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixProvider.java:189) | Fejl og exceptions | EntityType is required. |
| [217](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixProvider.java:217) | Fejl og exceptions | FromEntityId and EntityType are required. |
| [318](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixProvider.java:318) | Fejl og exceptions | CustomerId and ProjectId are required. |
| [341](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixProvider.java:341) | Fejl og exceptions | CustomerId, ProjectId and EntityType are required. |
| [385](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixProvider.java:385) | Fejl og exceptions | Interface save record is required. |
| [389](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixProvider.java:389) | Fejl og exceptions | FromEntityId and ToEntityId are required. |
| [393](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixProvider.java:393) | Fejl og exceptions | IrlId is required. |
| [433](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixProvider.java:433) | Fejl og exceptions | Could not load saved interface record. |
| [463](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixProvider.java:463) | Fejl og exceptions | FromEntityId and ToEntityId are required. |
| [622](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixProvider.java:622) | Fejl og exceptions | Web session is missing customer or project context. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java

14 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [54](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:54) | Fejl og exceptions | fromEntityId and toEntityId are required. |
| [61](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:61) | Fejl og exceptions | Interface was not found or has already been removed. |
| [66](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:66) | Fejl og exceptions | Failed to remove interface cell |
| [72](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:72) | Fejl og exceptions | Invalid import request |
| [84](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:84) | Fejl og exceptions | Missing interfaceMatrix element. |
| [104](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:104) | Fejl og exceptions | Failed to save interface cell |
| [110](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:110) | Fejl og exceptions | Invalid list request |
| [115](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:115) | Fejl og exceptions | Invalid edit request |
| [120](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:120) | Fejl og exceptions | Invalid create request |
| [143](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:143) | Fejl og exceptions | No EntityType configured for interface matrix endpoint:  |
| [156](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:156) | Fejl og exceptions | Missing required field: fromEntityId |
| [160](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:160) | Fejl og exceptions | Missing required field: toEntityId |
| [164](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:164) | Fejl og exceptions | Missing required field: irlId |
| [190](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/InterfaceMatrixServlet.java:190) | Fejl og exceptions | Unable to write error response |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/LsysInterfaceMatrixDocument.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [30](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/LsysInterfaceMatrixDocument.java:30) | Øvrige tekstkandidater | Interface Management |
| [35](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/LsysInterfaceMatrixDocument.java:35) | Øvrige tekstkandidater | To Logical Architecture |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/PsysInterfaceMatrixDocument.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [30](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/PsysInterfaceMatrixDocument.java:30) | Øvrige tekstkandidater | Interface Management |
| [35](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/PsysInterfaceMatrixDocument.java:35) | Øvrige tekstkandidater | To Physical Architecture |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/StkInterfaceMatrixDocument.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [32](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/StkInterfaceMatrixDocument.java:32) | Øvrige tekstkandidater | Interface Management |
| [37](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/StkInterfaceMatrixDocument.java:37) | Øvrige tekstkandidater | To Stakeholder Requirement |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/SysInterfaceMatrixDocument.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [32](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/SysInterfaceMatrixDocument.java:32) | Øvrige tekstkandidater | Interface Management |
| [37](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/interfacematrix/SysInterfaceMatrixDocument.java:37) | Øvrige tekstkandidater | To Systems Requirement |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureExporters.java

9 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [10](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureExporters.java:10) | Eksport og import | ID |
| [11](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureExporters.java:11) | Eksport og import | Level |
| [12](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureExporters.java:12) | Eksport og import | Name |
| [13](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureExporters.java:13) | Eksport og import | Description |
| [30](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureExporters.java:30) | Eksport og import | ChangedBy |
| [31](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureExporters.java:31) | Eksport og import | Changed |
| [32](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureExporters.java:32) | Eksport og import | Active |
| [102](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureExporters.java:102) | Eksport og import | Logical Architecture |
| [117](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureExporters.java:117) | Eksport og import | Logical Architecture Export |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureImporters.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [97](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureImporters.java:97) | Eksport og import | Import Logical Structures |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureInfo.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [46](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureInfo.java:46) | Fejl og exceptions | Invalid entity request type |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureServlet.java

8 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [51](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureServlet.java:51) | Fejl og exceptions | No entities imported |
| [92](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureServlet.java:92) | Fejl og exceptions | Failed to parse logical structure XML |
| [113](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureServlet.java:113) | Fejl og exceptions | Invalid overview request |
| [229](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureServlet.java:229) | Øvrige tekstkandidater | Logical Architecture |
| [234](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureServlet.java:234) | Øvrige tekstkandidater | Logical Architecture |
| [239](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureServlet.java:239) | Øvrige tekstkandidater | Logical Architecture |
| [244](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureServlet.java:244) | Øvrige tekstkandidater | Logical Architecture |
| [246](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/logicalstructure/LogicalStructureServlet.java:246) | Fejl og exceptions | Unsupported export format:  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/move/AbstractMoveEntity.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [108](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/move/AbstractMoveEntity.java:108) | Fejl og exceptions | Code  |
| [109](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/move/AbstractMoveEntity.java:109) | Fejl og exceptions |  is not the source parent or one of its children:  |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/move/EntityMoveServlet.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [58](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/move/EntityMoveServlet.java:58) | Øvrige tekstkandidater | The entity could not be moved due to :  |
| [113](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/move/EntityMoveServlet.java:113) | Fejl og exceptions | No provider configured for move endpoint:  |
| [119](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/move/EntityMoveServlet.java:119) | Fejl og exceptions | fromEntityId is required. |
| [122](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/move/EntityMoveServlet.java:122) | Fejl og exceptions | An entity cannot be moved to itself. |
| [125](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/move/EntityMoveServlet.java:125) | Fejl og exceptions | fromCode is required. |
| [130](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/move/EntityMoveServlet.java:130) | Fejl og exceptions | toEntityId and toCode must both identify an entity or both be null for Root. |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/nflpdiagram/RflpRelationDiagramServlet.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [27](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/nflpdiagram/RflpRelationDiagramServlet.java:27) | Fejl og exceptions | Invalid import request |
| [32](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/nflpdiagram/RflpRelationDiagramServlet.java:32) | Fejl og exceptions | Invalid save request |
| [37](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/nflpdiagram/RflpRelationDiagramServlet.java:37) | Fejl og exceptions | Invalid list request |
| [42](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/nflpdiagram/RflpRelationDiagramServlet.java:42) | Fejl og exceptions | Invalid edit request |
| [47](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/nflpdiagram/RflpRelationDiagramServlet.java:47) | Fejl og exceptions | Invalid create request |
| [59](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/pro/nflpdiagram/RflpRelationDiagramServlet.java:59) | Fejl og exceptions | Invalid export request |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/ProjectOverview.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [117](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/ProjectOverview.java:117) | Øvrige tekstkandidater | Over due |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/ProjectOverviewProvider.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [79](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/ProjectOverviewProvider.java:79) | Fejl og exceptions | No project selected in web session. |
| [90](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/ProjectOverviewProvider.java:90) | Fejl og exceptions | No latest project found for projectId:  |
| [284](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/ProjectOverviewProvider.java:284) | Øvrige tekstkandidater | Over due |
| [318](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/ProjectOverviewProvider.java:318) | Øvrige tekstkandidater | End date exceeded |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/ProjectOverviewServlet.java

5 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [39](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/ProjectOverviewServlet.java:39) | Fejl og exceptions | Invalid save request |
| [44](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/ProjectOverviewServlet.java:44) | Fejl og exceptions | Invalid list of entities request |
| [49](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/ProjectOverviewServlet.java:49) | Fejl og exceptions | Invalid edit entity request |
| [54](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/ProjectOverviewServlet.java:54) | Fejl og exceptions | Invalid create entity request |
| [59](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/ProjectOverviewServlet.java:59) | Fejl og exceptions | Invalid export request |

### web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/SrlProvider.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [49](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/SrlProvider.java:49) | Øvrige tekstkandidater | Level :  |
| [52](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/api/web/application/views/projectstatus/overview/SrlProvider.java:52) | Øvrige tekstkandidater | Level :  |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/cache/EhcacheProvider.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [52](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/cache/EhcacheProvider.java:52) | Fejl og exceptions | Missing ehcache.xml on classpath (expected at /ehcache.xml) |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/customer/CustomerServlet.java

4 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [30](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/customer/CustomerServlet.java:30) | Fejl og exceptions | Customer import is not supported. |
| [48](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/customer/CustomerServlet.java:48) | Fejl og exceptions | Customer list is not supported. |
| [82](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/customer/CustomerServlet.java:82) | Fejl og exceptions | Customer export is not supported. |
| [91](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/customer/CustomerServlet.java:91) | Fejl og exceptions | Customer overview is not supported. |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java

13 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [488](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java:488) | Fejl og exceptions | Invalid entity type :  |
| [499](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java:499) | Fejl og exceptions | Invalid entity type :  |
| [858](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java:858) | Øvrige tekstkandidater | code level |
| [873](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java:873) | Fejl og exceptions | Expected to update one  |
| [873](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java:873) | Fejl og exceptions |  record for entityId= |
| [874](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java:874) | Fejl og exceptions | , but updated  |
| [917](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java:917) | Fejl og exceptions | Could not copy entity to version  |
| [918](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java:918) | Fejl og exceptions |  for entityId= |
| [938](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java:938) | Fejl og exceptions | Could not copy entity history for entityId= |
| [1052](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java:1052) | Fejl og exceptions | Error creating entities |
| [1098](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java:1098) | Fejl og exceptions | Insert entity failed for entityId= |
| [1170](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java:1170) | Fejl og exceptions | Invalid data element type:  |
| [1177](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/entities/EntityProvider.java:1177) | Fejl og exceptions | Insert entity element failed for entityId= |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/AbstractField.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [101](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/AbstractField.java:101) | Fejl og exceptions | Unable to close field object  |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/lookups/codeselector/AbstractParentCodeSelector.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [80](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/lookups/codeselector/AbstractParentCodeSelector.java:80) | Fejl og exceptions | Not supported yet. |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/lookups/common/AbstractLookup.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [89](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/lookups/common/AbstractLookup.java:89) | Fejl og exceptions | Failed to lookup DataSource via JNDI name:  |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/lookups/common/ChangedBy.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [24](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/lookups/common/ChangedBy.java:24) | Øvrige tekstkandidater | Changed By |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/lookups/common/CreatedBy.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [32](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/lookups/common/CreatedBy.java:32) | Øvrige tekstkandidater | Created By |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/lookups/common/EntityRelationType.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [31](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/lookups/common/EntityRelationType.java:31) | Øvrige tekstkandidater | Relation Type |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/lookups/common/UserRole.java

1 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [32](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/fields/lookups/common/UserRole.java:32) | Øvrige tekstkandidater | User Type |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java

26 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [117](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:117) | Fejl og exceptions | Menu row is required. |
| [164](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:164) | Fejl og exceptions | Menu item was not found. |
| [217](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:217) | Fejl og exceptions | No menu rows found in import file. |
| [224](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:224) | Fejl og exceptions | Import requires unique, positive MenuIds. |
| [228](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:228) | Fejl og exceptions | Invalid MenuItemType for MenuId  |
| [231](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:231) | Fejl og exceptions | Invalid SubscriptionCode for MenuId  |
| [241](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:241) | Fejl og exceptions | Invalid parent hierarchy for MenuId  |
| [307](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:307) | Fejl og exceptions | Could not import menu items. |
| [316](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:316) | Fejl og exceptions | MenuId is required. |
| [326](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:326) | Fejl og exceptions | Menu item was not found. |
| [333](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:333) | Fejl og exceptions | Menu item is missing from its sibling list. |
| [468](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:468) | Fejl og exceptions | Insert menu item failed. |
| [478](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:478) | Fejl og exceptions | Could not read generated menu id. |
| [514](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:514) | Fejl og exceptions | Menu item not found. |
| [724](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:724) | Fejl og exceptions | MenuItemText is required. |
| [728](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:728) | Fejl og exceptions | MenuItemText must be maximum 50 characters. |
| [732](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:732) | Fejl og exceptions | MenuItemUrl must be maximum 50 characters. |
| [736](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:736) | Fejl og exceptions | UserRoles must be maximum 20 characters. |
| [740](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:740) | Fejl og exceptions | IconId is invalid. |
| [744](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:744) | Fejl og exceptions | MenuItemType is required. |
| [748](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:748) | Fejl og exceptions | SubscriptionCode is invalid. |
| [850](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:850) | Fejl og exceptions | Header menu items must be top-level. |
| [860](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:860) | Fejl og exceptions | Parent menu item was not found. |
| [864](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:864) | Fejl og exceptions | A menu item cannot be its own parent. |
| [868](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:868) | Fejl og exceptions | Parent menu item must be a top-level item. |
| [872](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/misc/MenuProvider.java:872) | Fejl og exceptions | Menu items with children cannot be moved under another parent. |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/project/MyProjectsServlet.java

8 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [157](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/MyProjectsServlet.java:157) | Fejl og exceptions | My Projects import is not supported. |
| [166](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/MyProjectsServlet.java:166) | Fejl og exceptions | My Projects save is not supported. |
| [188](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/MyProjectsServlet.java:188) | Fejl og exceptions | My Projects edit is not supported. |
| [198](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/MyProjectsServlet.java:198) | Fejl og exceptions | My Projects create is not supported. |
| [207](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/MyProjectsServlet.java:207) | Fejl og exceptions | My Projects export is not supported. |
| [298](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/MyProjectsServlet.java:298) | Øvrige tekstkandidater | My Projects |
| [490](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/MyProjectsServlet.java:490) | Øvrige tekstkandidater | Over due |
| [523](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/MyProjectsServlet.java:523) | Øvrige tekstkandidater | End date exceeded |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectProvider.java

5 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [347](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectProvider.java:347) | Fejl og exceptions | No latest project found for projectId:  |
| [433](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectProvider.java:433) | Fejl og exceptions | Could not insert project record. |
| [455](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectProvider.java:455) | Fejl og exceptions | Project record is required. |
| [459](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectProvider.java:459) | Fejl og exceptions | Project name is required. |
| [463](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectProvider.java:463) | Fejl og exceptions | Customer id is required. |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectServlet.java

7 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [51](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectServlet.java:51) | Fejl og exceptions | Project import is not supported. |
| [64](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectServlet.java:64) | Fejl og exceptions | Missing projectDocument in save payload. |
| [70](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectServlet.java:70) | Fejl og exceptions | Missing project in save payload. |
| [104](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectServlet.java:104) | Fejl og exceptions | Failed to save project XML. |
| [144](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectServlet.java:144) | Fejl og exceptions | Project export is not supported. |
| [251](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectServlet.java:251) | Fejl og exceptions | Invalid date value for  |
| [268](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/project/ProjectServlet.java:268) | Fejl og exceptions | Invalid decimal value for  |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java

24 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [99](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:99) | Korte tekstkandidater | Successful |
| [100](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:100) | Korte tekstkandidater | Failed |
| [104](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:104) | Øvrige tekstkandidater | MFA enabled |
| [105](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:105) | Øvrige tekstkandidater | Missing MFA |
| [109](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:109) | Øvrige tekstkandidater | Failed login |
| [110](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:110) | Øvrige tekstkandidater | Admin changes |
| [111](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:111) | Øvrige tekstkandidater | Locked account |
| [112](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:112) | Øvrige tekstkandidater | New user |
| [334](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:334) | Korte tekstkandidater | Failed |
| [421](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:421) | Øvrige tekstkandidater | Security score |
| [422](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:422) | Korte tekstkandidater | Warning |
| [422](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:422) | Korte tekstkandidater | Critical |
| [425](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:425) | Øvrige tekstkandidater | Failed logins |
| [426](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:426) | Korte tekstkandidater | Warning |
| [426](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:426) | Korte tekstkandidater | Critical |
| [429](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:429) | Øvrige tekstkandidater | Locked accounts |
| [430](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:430) | Korte tekstkandidater | Warning |
| [430](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:430) | Korte tekstkandidater | Critical |
| [433](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:433) | Øvrige tekstkandidater | MFA coverage |
| [434](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:434) | Korte tekstkandidater | Warning |
| [434](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:434) | Korte tekstkandidater | Critical |
| [437](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:437) | Øvrige tekstkandidater | Admin changes |
| [438](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:438) | Korte tekstkandidater | Warning |
| [438](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/AdminDashboardSecurityProvider.java:438) | Korte tekstkandidater | Critical |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/security/GeoIpService.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [19](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/GeoIpService.java:19) | Øvrige tekstkandidater | Local network |
| [30](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/security/GeoIpService.java:30) | Øvrige tekstkandidater | Private network |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileMainServlet.java

8 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [26](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileMainServlet.java:26) | Fejl og exceptions | My Profile import is not supported. |
| [37](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileMainServlet.java:37) | Fejl og exceptions | Missing user in save payload. |
| [43](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileMainServlet.java:43) | Fejl og exceptions | Current user was not found. |
| [75](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileMainServlet.java:75) | Fejl og exceptions | User could not be saved. |
| [89](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileMainServlet.java:89) | Fejl og exceptions | My profile list is not supported. |
| [118](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileMainServlet.java:118) | Fejl og exceptions | My profile create is not supported. |
| [123](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileMainServlet.java:123) | Fejl og exceptions | My profile export is not supported. |
| [137](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileMainServlet.java:137) | Fejl og exceptions | Forbidden |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileServlet.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [22](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileServlet.java:22) | Fejl og exceptions | My Profile import is not supported. |
| [34](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileServlet.java:34) | Fejl og exceptions | Current user could not be loaded. |
| [38](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileServlet.java:38) | Fejl og exceptions | Missing user in save payload. |
| [69](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileServlet.java:69) | Fejl og exceptions | My Profile could not be saved. |
| [118](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileServlet.java:118) | Fejl og exceptions | My Profile export is not supported. |
| [132](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/MyProfileServlet.java:132) | Fejl og exceptions | Forbidden |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/users/UserMainInfo.java

3 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [167](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/UserMainInfo.java:167) | Korte tekstkandidater | Selected |
| [588](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/UserMainInfo.java:588) | Korte tekstkandidater | Theme |
| [640](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/UserMainInfo.java:640) | Korte tekstkandidater | Light |

### web-server/src/main/java/com/bepa/eis/server/dataprovider/users/UserMainServlet.java

6 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [29](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/UserMainServlet.java:29) | Fejl og exceptions | User import is not supported. |
| [42](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/UserMainServlet.java:42) | Fejl og exceptions | Missing user in save payload. |
| [54](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/UserMainServlet.java:54) | Fejl og exceptions | User could not be saved. |
| [82](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/UserMainServlet.java:82) | Fejl og exceptions | Missing user id. |
| [101](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/UserMainServlet.java:101) | Fejl og exceptions | User export is not supported. |
| [115](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/dataprovider/users/UserMainServlet.java:115) | Fejl og exceptions | Forbidden |

### web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java

13 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [150](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java:150) | Fejl og exceptions | CustomerId is invalid :  |
| [165](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java:165) | Fejl og exceptions | ProjectId cannot be null |
| [171](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java:171) | Fejl og exceptions | ProjectId is invalid :  |
| [222](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java:222) | Fejl og exceptions | Entity Id Value is invalid :  |
| [284](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java:284) | Fejl og exceptions | Project Id is invalid :  |
| [287](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java:287) | Fejl og exceptions | Customer Id is invalid :  |
| [291](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java:291) | Fejl og exceptions | Unknown entity type :  |
| [291](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java:291) | Fejl og exceptions | null |
| [294](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java:294) | Fejl og exceptions | Entity Id is invalid :  |
| [297](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java:297) | Fejl og exceptions | Version is invalid :  |
| [301](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java:301) | Fejl og exceptions | List of data elements is empty:  |
| [305](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java:305) | Fejl og exceptions | Changed By Id is invalid :  |
| [309](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/AbstractEntity.java:309) | Fejl og exceptions | Changed Date is invalid :  |

### web-server/src/main/java/com/bepa/eis/server/entites/configuration/EntityConfiguration.java

3 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [46](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/configuration/EntityConfiguration.java:46) | Fejl og exceptions | Duplicate entity type found:  |
| [56](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/configuration/EntityConfiguration.java:56) | Fejl og exceptions | Duplicate entity data element type found:  |
| [65](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/configuration/EntityConfiguration.java:65) | Fejl og exceptions | Duplicate entity data element type found:  |

### web-server/src/main/java/com/bepa/eis/server/entites/datatypes/AbstractDataElement.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [40](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/datatypes/AbstractDataElement.java:40) | Fejl og exceptions | EntityDataElement is null for field:  |
| [108](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/datatypes/AbstractDataElement.java:108) | Øvrige tekstkandidater | EntityDataElement is null |

### web-server/src/main/java/com/bepa/eis/server/entites/datatypes/LocalDateDataElement.java

2 forekomster.

| Linje | Kategori | Tekst |
|---:|---|---|
| [25](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/datatypes/LocalDateDataElement.java:25) | Fejl og exceptions | LocalDate < |
| [25](/C:/udv/EIS/web-server/src/main/java/com/bepa/eis/server/entites/datatypes/LocalDateDataElement.java:25) | Fejl og exceptions | >is invalid :  |

