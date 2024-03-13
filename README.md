# economic-crime-levy-registration

This is the backend microservice that stores transient customer registration data for the Economic Crime Levy, before the data
is then submitted to head of duty, ETMP. 
The service provides APIs to be consumed by [economic-crime-levy-registration-frontend](https://github.com/hmrc/economic-crime-levy-registration-frontend) microservice.

## API Endpoints

- Create/update registration: `PUT /economic-crime-levy-registration/registrations`  
- Get registration: `GET /economic-crime-levy-registration/registrations/:id`  
- Delete registration: `DELETE /economic-crime-levy-registration/registrations/:id`
- [Get subscription status](api-docs/get-subscription-status.md): `GET /economic-crime-levy-registration/subscription-status/:businessPartnerId`
- [Get registration validation errors](api-docs/get-registration-validation-errors.md): `GET /economic-crime-levy-registration/registrations/:id/validation-errors`

## Running the service

> `sbt run`

The service runs on port `14001` by default.

## Running dependencies

Using [service manager](https://github.com/hmrc/service-manager)
with the service manager profile `ECONOMIC_CRIME_LEVY_ALL` will start
all of the Economic Crime Levy microservices as well as the services
that they depend on.

> `sm --start ECONOMIC_CRIME_LEVY_ALL`

## Running tests

### Unit tests

> `sbt test`

### Integration tests

> `sbt it:test`

### All tests

This is a sbt command alias specific to this project. It will run a scala format
check, run a scala style check, run unit tests, run integration tests and produce a coverage report.
> `sbt runAllChecks`

## Scalafmt and Scalastyle

To check if all the scala files in the project are formatted correctly:
> `sbt scalafmtCheckAll`

To format all the scala files in the project correctly:
> `sbt scalafmtAll`

To check if there are any scalastyle errors, warnings or infos:
> `sbt scalastyle`

## Feature flags

- enrolmentStoreProxyStubEnabled: When enabled we use the stub for Enrolment Store Proxy rather than the actual service.
- taxEnrolmentsFailureStubEnabled: When enabled we use the stub for Tax Enrolments rather than the actual service.
- amendRegistrationNrsEnabled: Controls submitting to NRS when submitting an amendment. The DMS submission will still occur if this is switched off. It only pertains to the NRS service.
- nrsSubmissionEnabled: Controls submitting to NRS when submitting a registration for the first time. The DMS submission will still occur if this is switched off. It only pertains to the NRS service.

### License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").