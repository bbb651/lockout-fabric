package me.marin.lockout;

import me.marin.lockout.client.LockoutClient;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static me.marin.lockout.Constants.*;
import static me.marin.lockout.client.gui.BoardBuilderScreen.CENTER_OFFSET;

public class Utility {

    public static void drawBingoBoard(DrawContext context, int x, int y) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        context.drawTexture(Constants.GUI_IDENTIFIER, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        x += GUI_FIRST_ITEM_OFFSET;
        y += GUI_FIRST_ITEM_OFFSET;
        final int startX = x;

        Lockout lockout = LockoutClient.lockout;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                Goal goal = lockout.getBoard().getGoals().get(j + 5 * i);
                if (goal != null) {
                    if (goal.isCompleted()) {
                        context.fill(x, y, x + 16, y + 16, (0xFF << 24) | goal.getCompletedTeam().getColor().getColorValue());
                    }

                    goal.render(context, textRenderer, x, y);

                }
                x += GUI_ITEM_SLOT_SIZE;
            }
            y += GUI_ITEM_SLOT_SIZE;
            x = startX;
        }
        x += 2;
        y += 1;
        List<String> pointsList = new ArrayList<>();
        for (LockoutTeam team : lockout.getTeams()) {
            pointsList.add(team.getColor() + "" + team.getPoints() + "" + Formatting.RESET);
        }

        context.drawText(textRenderer, String.join(Formatting.RESET + "" + Formatting.GRAY + "-", pointsList), x, y, 0, true);
        if (lockout.hasStarted()) {
            long duration = (lockout.isRunning() ? System.currentTimeMillis() : lockout.getEndTime()) - lockout.getStartTime();
            long second = (duration / 1000) % 60;
            long minute = (duration / (1000 * 60)) % 60;
            long hour = (duration / (1000 * 60 * 60)) % 24;

            String time;
            if (hour > 0) {
                time = String.format("%02d:%02d:%02d", hour, minute, second);
            } else {
                time = String.format("%02d:%02d", minute, second);
            }

            context.drawText(textRenderer, Formatting.WHITE + time, context.getScaledWindowWidth() - textRenderer.getWidth(time) - 4, y, 0, true);
        }

        List<String> formattedNames = new ArrayList<>();
        int maxWidth = 0;
        for (LockoutTeam team : lockout.getTeams()) {
            for (String playerName : team.getPlayerNames()) {
                formattedNames.add(team.getColor() + playerName);
                maxWidth = Math.max(maxWidth, textRenderer.getWidth(playerName));
            }
        }

        y += 20;
        context.fill(context.getScaledWindowWidth() - maxWidth - 4,  y - 2, context.getScaledWindowWidth() - 1, y + formattedNames.size() * textRenderer.fontHeight + 1, 0x80_00_00_00);

        for (String formattedName : formattedNames) {
            context.drawText(textRenderer, formattedName, context.getScaledWindowWidth() - textRenderer.getWidth(formattedName) - 2, y, 0, true);
            y += textRenderer.fontHeight;
        }
    }

    public static void drawCenterBingoBoard(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        int x = width / 2 - Constants.GUI_CENTER_WIDTH / 2;
        int y = height / 2 - Constants.GUI_CENTER_HEIGHT / 2;

        context.drawTexture(GUI_CENTER_IDENTIFIER, x, y, 0, 0, GUI_CENTER_WIDTH, GUI_CENTER_HEIGHT, GUI_CENTER_WIDTH, GUI_CENTER_HEIGHT);

        x += GUI_CENTER_FIRST_ITEM_OFFSET_X;
        y += GUI_CENTER_FIRST_ITEM_OFFSET_Y;
        final int startX = x;

        Goal hoveredGoal = getBoardHoveredGoal(context, mouseX, mouseY);

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                Goal goal = LockoutClient.lockout.getBoard().getGoals().get(j + 5 * i);
                if (goal != null) {
                    if (goal.isCompleted()) {
                        context.fill(x, y, x + 16, y + 16, (0xFF << 24) | goal.getCompletedTeam().getColor().getColorValue());
                    }

                    goal.render(context, textRenderer, x, y);

                    if (goal == hoveredGoal) {
                        context.fill(x, y, x + 16, y + 16, 400, -2130706433);
                    }
                }
                x += GUI_CENTER_ITEM_SLOT_SIZE;
            }
            y += GUI_CENTER_ITEM_SLOT_SIZE;
            x = startX;
        }
    }

    public static Optional<Integer> getBoardHoveredIndex(int width, int height, int mouseX, int mouseY) {
        int x = width / 2 - Constants.GUI_CENTER_WIDTH / 2 + GUI_CENTER_FIRST_ITEM_OFFSET_X - CENTER_OFFSET;
        int y = height / 2 - Constants.GUI_CENTER_HEIGHT / 2 + GUI_CENTER_FIRST_ITEM_OFFSET_Y;
        final int startX = x;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (mouseX >= x-1 && mouseX < x+18 && mouseY >= y-1 && mouseY < y+18) {
                    return Optional.of(j + i * 5);
                }
                x += GUI_CENTER_ITEM_SLOT_SIZE;
            }
            y += GUI_CENTER_ITEM_SLOT_SIZE;
            x = startX;
        }

        return Optional.empty();
    }

    public static Goal getBoardHoveredGoal(DrawContext context, int mouseX, int mouseY) {
        Optional<Integer> hoveredIdx = getBoardHoveredIndex(context.getScaledWindowWidth(), context.getScaledWindowHeight(), mouseX, mouseY);
        return hoveredIdx.map(integer -> LockoutClient.lockout.getBoard().getGoals().get(integer)).orElse(null);
    }

    public static void drawGoalInformation(DrawContext context, TextRenderer textRenderer, Goal goal, int mouseX, int mouseY) {
        List<OrderedText> lore = new ArrayList<>();
        lore.add(Text.of(((goal instanceof HasTooltipInfo) ? Formatting.UNDERLINE : "") + goal.getGoalName()).asOrderedText());
        if (goal instanceof HasTooltipInfo) {
            String s = LockoutClient.goalLoreMap.get(goal.getId());
            if (s != null) {
                for (String t : s.split("\n")) {
                    lore.add(Text.of(t).asOrderedText());
                }
            }
        }
        context.drawOrderedTooltip(textRenderer, lore, mouseX, mouseY);
    }

}
