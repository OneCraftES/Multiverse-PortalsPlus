package org.mvplugins.multiverse.portals.action;

import org.mvplugins.multiverse.core.utils.result.FailureReason;

public interface ActionFailureReason extends FailureReason {
    ActionFailureReason INSTANCE = new ActionFailureReason() {};
}
