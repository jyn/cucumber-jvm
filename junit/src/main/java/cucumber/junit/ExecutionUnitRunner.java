package cucumber.junit;

import cucumber.runtime.CucumberException;
import cucumber.runtime.Runtime;
import cucumber.runtime.model.CucumberScenario;
import gherkin.formatter.model.Step;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.util.List;

/**
 * Runs a scenario, or a "synthetic" scenario derived from an Examples row.
 */
class ExecutionUnitRunner extends ParentRunner<Step> {
    private final Runtime runtime;
    private final List<String> gluePaths;
    private final CucumberScenario cucumberScenario;
    private final JUnitReporter jUnitReporter;

    public ExecutionUnitRunner(Runtime runtime, List<String> gluePaths, CucumberScenario cucumberScenario, JUnitReporter jUnitReporter) throws InitializationError {
        super(ExecutionUnitRunner.class);
        this.runtime = runtime;
        this.gluePaths = gluePaths;
        this.cucumberScenario = cucumberScenario;
        this.jUnitReporter = jUnitReporter;
    }

    @Override
    protected List<Step> getChildren() {
        return cucumberScenario.getSteps();
    }

    @Override
    public String getName() {
        return cucumberScenario.getVisualName();
    }

    @Override
    protected Description describeChild(Step step) {
        return Description.createSuiteDescription(step.getKeyword() + step.getName());
    }

    @Override
    public void run(RunNotifier notifier) {
        jUnitReporter.setStepParentRunner(this, notifier);
        try {
            cucumberScenario.buildWorldAndRunBeforeHooks(gluePaths, runtime);

            /*
               We're running the background without reporting the steps as junit children - we don't want them to show up in the
               junit report. However, if any of the background steps fail, we mark the entire scenario as failed. Scenario steps
               will be skipped.
            */
            Throwable failure = cucumberScenario.runBackground(jUnitReporter.getFormatter(), jUnitReporter.getReporter());
            if (failure != null) {
                notifier.fireTestFailure(new Failure(getDescription(), failure));
            }
            cucumberScenario.format(jUnitReporter);
        } catch (Throwable e) {
            // If a Before hook fails...
            notifier.fireTestFailure(new Failure(getDescription(), e));
        }
        // Run the steps
        super.run(notifier);
        try {
            cucumberScenario.runAfterHooksAndDisposeWorld();
        } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(getDescription(), e));
        }
    }

    @Override
    protected void runChild(Step step, RunNotifier notifier) {
        cucumberScenario.runStep(step, jUnitReporter);
    }
}
