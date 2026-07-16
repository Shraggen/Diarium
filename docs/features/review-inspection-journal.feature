@offline @journal @retrieval
Feature: Review the inspection journal
  As a beekeeper
  I want confirmed inspections returned in reverse chronological order
  So that the most recent field note is easiest to find

  Scenario: A newly confirmed inspection appears first
    Given hive 4 was confirmed earlier
    And hive 5 was confirmed later
    When recent inspections are requested
    Then hive 5 appears before hive 4

  Scenario: Planning and cancellation do not create journal entries
    Given the journal is empty
    When an inspection is planned and cancelled
    Then the journal remains empty
