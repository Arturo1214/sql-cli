package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

@Deprecated
public final class TuiEventRouter {

    private TuiEventRouter() {}

    public static WorkspaceAction route(KeyStroke keyStroke) {
        return route(keyStroke, false);
    }

    public static WorkspaceAction route(KeyStroke keyStroke, boolean previousWasEnter) {
        return route(keyStroke, WorkspaceScreenState.initial(), previousWasEnter);
    }

    public static WorkspaceAction route(KeyStroke keyStroke, WorkspaceScreenState state, boolean previousWasEnter) {
        if (keyStroke == null) {
            return WorkspaceAction.ignore();
        }
        if (state == null) {
            state = WorkspaceScreenState.initial();
        }

        KeyType keyType = keyStroke.getKeyType();
        if (state.creationFormVisible()) {
            if (keyType == KeyType.Escape || isControlCharacter(keyStroke, 'c')) {
                return WorkspaceAction.closeModal();
            }
            if (keyType == KeyType.Enter) {
                return WorkspaceAction.advanceFormField();
            }
            if (keyType == KeyType.Backspace) {
                return WorkspaceAction.deleteFormCharacter();
            }
            if (keyStroke.getCharacter() != null && !keyStroke.isCtrlDown()) {
                return WorkspaceAction.updateFormField(String.valueOf(keyStroke.getCharacter()));
            }
            return WorkspaceAction.ignore();
        }
        if (keyType == KeyType.Escape || isControlCharacter(keyStroke, 'c')) {
            if (keyType == KeyType.Escape && (state.helpVisible() || state.creationFormVisible())) {
                return WorkspaceAction.closeModal();
            }
            return WorkspaceAction.exit();
        }
        if (keyType == KeyType.Enter) {
            if (state.creationFormVisible()) {
                return WorkspaceAction.submitForm();
            }
            if (state.focus() == WorkspaceFocus.CONNECTIONS) {
                return WorkspaceAction.selectCurrent();
            }
            return WorkspaceAction.editorEnter();
        }
        if (keyType == KeyType.ArrowDown || isPlainCharacter(keyStroke, 'j')) {
            return WorkspaceAction.moveSelection(1);
        }
        if (keyType == KeyType.ArrowUp || isPlainCharacter(keyStroke, 'k')) {
            return WorkspaceAction.moveSelection(-1);
        }
        if (keyType == KeyType.Tab) {
            return WorkspaceAction.nextFocus();
        }
        if (keyType == KeyType.ReverseTab) {
            return WorkspaceAction.previousFocus();
        }
        if (
            keyType == KeyType.F2 ||
            isControlCharacter(keyStroke, 'b') ||
            isControlCharacter(keyStroke, 'j') ||
            (keyType == KeyType.ArrowLeft && keyStroke.isCtrlDown())
        ) {
            return WorkspaceAction.focus(WorkspaceFocus.CONNECTIONS);
        }
        if (
            keyType == KeyType.F3 ||
            isControlCharacter(keyStroke, 'e') ||
            isControlCharacter(keyStroke, 'l') ||
            (keyType == KeyType.ArrowRight && keyStroke.isCtrlDown())
        ) {
            return WorkspaceAction.focus(WorkspaceFocus.EDITOR);
        }
        if (isControlCharacter(keyStroke, 'r')) {
            return WorkspaceAction.executeCurrentStatement();
        }
        if (isControlCharacter(keyStroke, 'h')) {
            return WorkspaceAction.toggleHelp();
        }
        return WorkspaceAction.ignore();
    }

    private static boolean isControlCharacter(KeyStroke keyStroke, char expected) {
        Character character = keyStroke.getCharacter();
        return keyStroke.isCtrlDown() && character != null && Character.toLowerCase(character) == expected;
    }

    private static boolean isPlainCharacter(KeyStroke keyStroke, char expected) {
        Character character = keyStroke.getCharacter();
        return !keyStroke.isCtrlDown() && character != null && Character.toLowerCase(character) == expected;
    }
}
