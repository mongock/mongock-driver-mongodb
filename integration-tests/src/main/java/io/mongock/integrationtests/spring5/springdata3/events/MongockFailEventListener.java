package io.mongock.integrationtests.spring5.springdata3.events;
import io.mongock.runner.springboot.base.events.SpringMigrationFailureEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class MongockFailEventListener implements ApplicationListener<SpringMigrationFailureEvent> {
//
    @Override
    public void onApplicationEvent(SpringMigrationFailureEvent event) {
        System.out.println("[EVENT LISTENER] - Mongock finished with failures");
    }

}