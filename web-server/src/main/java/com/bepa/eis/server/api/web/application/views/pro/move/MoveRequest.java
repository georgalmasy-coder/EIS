package com.bepa.eis.server.api.web.application.views.pro.move;

public record MoveRequest(
        Integer fromEntityId,
        String fromCode,
        String fromName,
        Integer toEntityId,
        String toCode,
        String toName
) {

    public String toString() {
        String logMessage;
        if (toEntityId() != null) {
            logMessage =  String.format("Move from %s %s %s to %s %s %s", fromEntityId(), fromCode(), fromName(), toEntityId(), toCode(), toName());
        } else {
            logMessage = String.format("Move from %s %s %s to Root", fromEntityId(), fromCode(), fromName());
        }
        return logMessage;
    }
}

