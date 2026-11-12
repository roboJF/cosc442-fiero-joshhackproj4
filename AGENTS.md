# JoshHack — AI Agent Instructions

## 1. Project Environment

- This project uses Java 17 and Maven with the Maven Wrapper.
- JUnit 5 is used for automated regression testing.
- PIT is used for mutation testing.
- SpotBugs is used for static analysis.
- VS Code Java test coverage tools are used to measure code coverage.
- GitHub Actions runs the automated quality checks, including JUnit, PIT, and SpotBugs with SARIF reporting.

## 2. Testing and Quality Requirements

- Run relevant JUnit 5 tests after making changes to the project.
- Run the complete JUnit regression suite before considering software-quality work complete.
- Run PIT mutation testing and SpotBugs analysis when evaluating completed quality improvements.
- Review code coverage when adding or modifying tests.
- Verify that all relevant tests pass before reporting that a task is complete.
- Report any test failures, static-analysis findings, or unresolved issues instead of ignoring them.

## 3. Preserve Existing Functionality

- Preserve the existing behavior of JoshHack unless a change is explicitly requested and approved.
- Avoid unnecessary modifications to unrelated files or functionality.
- Do not remove, weaken, or disable existing tests simply to make the build pass.
- Do not bypass quality checks or reduce testing requirements to hide failures.

## 4. Production Code Changes

- Clearly identify any proposed changes to production code.
- Explain why production-code modifications are necessary before implementing them.
- Obtain human approval before modifying production code.
- Keep changes focused on the requested improvement.

## 5. Git and GitHub Restrictions

- Do not automatically commit, push, merge, or publish changes.
- Leave all Git commits and pushes for human review and approval.
- Do not modify or disable existing GitHub Actions workflows unless explicitly requested.

## 6. Agent Responsibilities

- Follow the instructions and scope of the assigned task.
- Inspect the relevant code and existing tests before making changes.
- Make only necessary changes that support the requested goal.
- Run appropriate quality checks and verify the results.
- Provide a summary of files changed, tests executed, and results.
- Stop and request clarification or approval when a task requires changes outside the approved scope.