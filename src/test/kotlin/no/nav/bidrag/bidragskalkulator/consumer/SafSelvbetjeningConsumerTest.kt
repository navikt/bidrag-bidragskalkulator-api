package no.nav.bidrag.bidragskalkulator.consumer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach


class SafSelvbetjeningConsumerTest {

    @BeforeEach
    fun setUp() {
        // Initialize any necessary components or mocks here
    }
}

val mockResponse = """
        {
          "data": {
            "dokumentoversiktSelvbetjening": {
              "journalposter": [
                {
                  "tema": "BID",
                  "tittel": "INNGÅENDE osadasdasm barnebidrag (uten dokumentdato)",
                  "datoSortering": "2022-02-09T12:11:13",
                  "journalpostId": "454000114",
                  "mottaker": null,
                  "avsender": {
                    "navn": "Hansen, Per"
                  },
                  "dokumenter": [
                    {
                      "dokumentInfoId": "454408109",
                      "tittel": "Vedtak om barnebidrag (test)",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    }
                  ]
                },
                {
                  "tema": "BID",
                  "tittel": "TEST2 osadasdasm barnebidrag (uten dokumentdato)",
                  "datoSortering": "2022-02-09T12:11:13",
                  "journalpostId": "454000113",
                  "mottaker": null,
                  "avsender": {
                    "navn": "Hansen, Per"
                  },
                  "dokumenter": [
                    {
                      "dokumentInfoId": "454408108",
                      "tittel": "Vedtak om barnebidrag (test)",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    }
                  ]
                },
                {
                  "tema": "BID",
                  "tittel": "TEST osadasdasm barnebidrag (uten dokumentdato)",
                  "datoSortering": "2025-07-01T13:15:41",
                  "journalpostId": "454000106",
                  "mottaker": {
                    "navn": "Hansen, Per"
                  },
                  "avsender": null,
                  "dokumenter": [
                    {
                      "dokumentInfoId": "454408101",
                      "tittel": "Vedtak om barnebidrag (test)",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    }
                  ]
                },
                {
                  "tema": "BID",
                  "tittel": "Vedtak barnebidrag",
                  "datoSortering": "2025-07-01T13:07:13",
                  "journalpostId": "454000102",
                  "mottaker": {
                    "navn": "Selvhjulpen Mandag"
                  },
                  "avsender": null,
                  "dokumenter": [
                    {
                      "dokumentInfoId": "454408093",
                      "tittel": "Vedtak barnebidrag",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    },
                    {
                      "dokumentInfoId": "454408094",
                      "tittel": "Vedtak barnebidrag",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    },
                    {
                      "dokumentInfoId": "454408095",
                      "tittel": "Information to the custodial parent when one of the parties is living abroad (Vedlegg til vedtak bidragsmottaker)",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    },
                    {
                      "dokumentInfoId": "454408096",
                      "tittel": "Antwortformular für Unterhaltspflichtige (Svarskjema bidragspliktig)",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    },
                    {
                      "dokumentInfoId": "454408097",
                      "tittel": "Bidragsforskudd, Saksbehandlingsnotat",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    }
                  ]
                },
                {
                  "tema": "BID",
                  "tittel": "Vedtak barnebidrag",
                  "datoSortering": "2025-06-27T12:43:26",
                  "journalpostId": "453999638",
                  "mottaker": {
                    "navn": "Selvhjulpen Mandag"
                  },
                  "avsender": null,
                  "dokumenter": [
                    {
                      "dokumentInfoId": "454407595",
                      "tittel": "Vedtak barnebidrag",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    }
                  ]
                },
                {
                  "tema": "BID",
                  "tittel": "Barnebidrag, Saksbehandlingsnotat (fra bidragsmottaker sak 2500056)",
                  "datoSortering": "2025-06-27T09:11:46",
                  "journalpostId": "453999556",
                  "mottaker": {
                    "navn": "Selvhjulpen Mandag"
                  },
                  "avsender": null,
                  "dokumenter": [
                    {
                      "dokumentInfoId": "454407507",
                      "tittel": "Barnebidrag, Saksbehandlingsnotat (fra bidragsmottaker sak 2500056)",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    }
                  ]
                },
                {
                  "tema": "BID",
                  "tittel": "Svar på forhåndsvarsel i sak om barnebidrag til bidragsmottaker",
                  "datoSortering": "2025-06-24T11:17:17",
                  "journalpostId": "453998552",
                  "mottaker": {
                    "navn": "Selvhjulpen Mandag"
                  },
                  "avsender": null,
                  "dokumenter": [
                    {
                      "dokumentInfoId": "454406332",
                      "tittel": "Svar på forhåndsvarsel i sak om barnebidrag til bidragsmottaker",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    }
                  ]
                },
                {
                  "tema": "BID",
                  "tittel": "Bidragsforskudd, Saksbehandlingsnotat",
                  "datoSortering": "2025-06-24T09:24:44",
                  "journalpostId": "453998503",
                  "mottaker": {
                    "navn": "Selvhjulpen Mandag"
                  },
                  "avsender": null,
                  "dokumenter": [
                    {
                      "dokumentInfoId": "454406259",
                      "tittel": "Bidragsforskudd, Saksbehandlingsnotat",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    },
                    {
                      "dokumentInfoId": "454406260",
                      "tittel": "WNIOSEK O DORĘCZENIE ZA GRANICĄ DOKUMENTÓW SĄDOWYCH I POZASĄDOWYCH (Forkynningsskjema (Polsk, Fransk, Engelsk))",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    }
                  ]
                },
                {
                  "tema": "BID",
                  "tittel": "Søknadsskjema for bidragspliktig",
                  "datoSortering": "2025-03-09T10:17:46",
                  "journalpostId": "453969746",
                  "mottaker": null,
                  "avsender": {
                    "navn": "SELVHJULPEN, MANDAG"
                  },
                  "dokumenter": [
                    {
                      "dokumentInfoId": "454373261",
                      "tittel": "Søknadsskjema for bidragspliktig",
                      "dokumentvarianter": [
                        {
                          "variantformat": "ARKIV"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          }
        }
    """.trimIndent()