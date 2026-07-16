@offline @journal @record_inspection
Feature: Record a hive inspection
  As a beekeeper working in the field
  I want a spoken or typed inspection note converted into a reviewable action
  So that the journal is fast to use without silently saving incorrect facts

  Rule: Planning never writes to the journal

    Scenario Outline: Plan an explicit inspection in a supported language
      Given the journal is empty
      And the command is "<command>"
      When the command is planned
      Then the proposed hive identifier is "<hive_id>"
      And the proposed queen observation is "<queen_seen>"
      And the journal remains empty

      Examples:
        | command                                                        | hive_id | queen_seen |
        | I inspected hive 4 and saw the queen.                          | 4       | true       |
        | Ich habe Bienenstock 5 kontrolliert und die Königin gesehen.   | 5       | true       |
        | Pregledao sam košnicu 6 i nisam video maticu.                  | 6       | false      |
        | Прегледао сам кошницу 7 и видео сам матицу.                    | 7       | true       |

  Rule: Only an explicit confirmation may write

    Scenario: Confirm a complete proposal
      Given a complete proposal for hive 4 with the queen seen
      When the proposal is confirmed
      Then exactly one inspection is stored for hive 4

    Scenario: Cancel a complete proposal
      Given a complete proposal for hive 4 with the queen seen
      When the proposal is cancelled
      Then the journal remains empty

  Rule: Uncertain facts are omitted rather than guessed

    Scenario: Two different hive identifiers are spoken
      Given the command is "I inspected hive 4, correction, hive 5, and saw the queen."
      When the command is planned
      Then no hive identifier is proposed
      And confirmation cannot persist an inspection

    Scenario: The queen observation is hedged
      Given the command is "I inspected hive 4 and maybe saw the queen."
      When the command is planned
      Then no queen observation is proposed
      And confirmation cannot persist an inspection
