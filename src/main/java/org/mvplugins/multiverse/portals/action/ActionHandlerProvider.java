package org.mvplugins.multiverse.portals.action;

import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.core.locale.message.Message;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.external.vavr.control.Option;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public final class ActionHandlerProvider {

    private final Map<String, ActionHandlerType<?, ?>> handlerTypeMap = new HashMap<>();

    public void registerHandlerType(ActionHandlerType<?, ?> handlerType) {
        handlerTypeMap.put(handlerType.getName(), handlerType);
    }

    public Collection<String> getAllHandlerTypes() {
        return handlerTypeMap.keySet();
    }

    public Attempt<? extends ActionHandlerType<?, ?>, ActionFailureReason> getHandlerType(String name) {
        return Option.of(handlerTypeMap.get(name))
                .map(Attempt::<ActionHandlerType<?, ?>, ActionFailureReason>success)
                .getOrElse(() -> Attempt.failure(ActionFailureReason.INSTANCE, Message.of("Unknown action type: " + name)));
    }

    public Attempt<? extends ActionHandler<?, ?>, ActionFailureReason> parseHandler(String actionType, String action) {
        return getHandlerType(actionType)
                .mapAttempt(actionHandlerType -> actionHandlerType.parseHandler(action));
    }
}
