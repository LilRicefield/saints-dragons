package com.leon.saintsdragons.server.entity.npc.dialogue;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class DialogueValidator {
    private DialogueValidator() {
    }

    public static DialogueValidationResult validate(DialogueDefinition dialogue) {
        if (!dialogue.nodes().containsKey(dialogue.start())) {
            return DialogueValidationResult.error("start node '" + dialogue.start() + "' does not exist");
        }
        for (var entry : dialogue.nodes().entrySet()) {
            DialogueDefinition.Node node = entry.getValue();
            if (node.isEnd()) {
                continue;
            }
            if (node.choices().isEmpty()) {
                return DialogueValidationResult.error("node '" + entry.getKey() + "' has no choices and is not end_dialogue");
            }
            for (DialogueDefinition.Choice choice : node.choices()) {
                DialogueTargetReference target = DialogueTargetReference.parse(dialogue.id(), choice.next());
                if (target == null) {
                    return DialogueValidationResult.error("node '" + entry.getKey() + "' has invalid next target '" + choice.next() + "'");
                }
                if (!target.external() && !dialogue.nodes().containsKey(target.nodeId())) {
                    return DialogueValidationResult.error("node '" + entry.getKey() + "' points to missing node '" + choice.next() + "'");
                }
            }
        }
        Set<String> reachable = reachableNodes(dialogue);
        for (String node : dialogue.nodes().keySet()) {
            if (!reachable.contains(node)) {
                return DialogueValidationResult.error("node '" + node + "' is unreachable from start node '" + dialogue.start() + "'");
            }
            if (!canReachEnd(dialogue, node, new HashSet<>())) {
                return DialogueValidationResult.error("node '" + node + "' cannot reach an end_dialogue node");
            }
        }
        return DialogueValidationResult.success();
    }

    private static Set<String> reachableNodes(DialogueDefinition dialogue) {
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(dialogue.start());
        while (!queue.isEmpty()) {
            String nodeId = queue.removeFirst();
            if (!visited.add(nodeId)) {
                continue;
            }
            DialogueDefinition.Node node = dialogue.nodes().get(nodeId);
            if (node == null || node.isEnd()) {
                continue;
            }
            for (DialogueDefinition.Choice choice : node.choices()) {
                DialogueTargetReference target = DialogueTargetReference.parse(dialogue.id(), choice.next());
                if (target != null && !target.external()) {
                    queue.add(target.nodeId());
                }
            }
        }
        return visited;
    }

    private static boolean canReachEnd(DialogueDefinition dialogue, String nodeId, Set<String> visiting) {
        DialogueDefinition.Node node = dialogue.nodes().get(nodeId);
        if (node == null) {
            return false;
        }
        if (node.isEnd()) {
            return true;
        }
        if (!visiting.add(nodeId)) {
            return false;
        }
        for (DialogueDefinition.Choice choice : node.choices()) {
            DialogueTargetReference target = DialogueTargetReference.parse(dialogue.id(), choice.next());
            if (target != null && target.external()) {
                return true;
            }
            if (target != null && canReachEnd(dialogue, target.nodeId(), new HashSet<>(visiting))) {
                return true;
            }
        }
        return false;
    }
}
