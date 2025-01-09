package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "javarushmarathon3_bot"; //TODO: добавь имя бота в кавычках
    public static final String TELEGRAM_BOT_TOKEN = "7702823502:AAFAioPvrhrmMWzIbuM3itnI-RpJX2WbOvU"; //TODO: добавь токен бота в кавычках
    public static final String OPEN_AI_TOKEN = "gpt:4dws6NYyD0BDK2ufp71ZJFkblB3TCC3tppbmX6OYmhSFydbM"; //TODO: добавь токен ChatGPT в кавычках

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    private ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentMode = null;
    private ArrayList<String> list = new ArrayList<>();

    private UserInfo me;
    private UserInfo she;
    private int questionCount;

    @Override
    public void onUpdateEventReceived(Update update) {
        //TODO: основной функционал бота будем писать здесь
        String message = getMessageText();

        if (message.equals("/start")) {
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            String txt = loadMessage("main");
            sendTextMessage("*It's*, _Crazy Bot!_");
            sendTextMessage(txt);

            showMainMenu("Главное меню бота", "/start",
                    "Генерация Tinder-профиля \uD83D\uDE0E", "/profile",
                    "Сообщение для знакомства \uD83E\uDD70", "/opener",
                    "Переписка от вашего имени \uD83D\uDE08", "/message",
                    "Переписка со звездами \uD83D\uDD25", "/date",
                    "Задать вопрос чату GPT \uD83E\uDDE0", "/gpt");
            return;
        }

        // command GPT
        if (message.equals("/gpt")) {
            currentMode = DialogMode.GPT;
            String txt = loadMessage("gpt");
            sendPhotoMessage("gpt");
            sendTextMessage(txt);
            return;
        }

        if (currentMode == DialogMode.GPT && !isMessageCommand()) {
            String prompt = loadPrompt("gpt");
            String answer = chatGPT.sendMessage(prompt, message);

            Message msg = sendTextMessage("Please, wait...");
            updateTextMessage(msg, answer);
            return;
        }

        // command DATE
        if (message.equals("/date")) {
            currentMode = DialogMode.DATE;
            String txt = loadMessage("date");
            sendPhotoMessage("date");
            sendTextButtonsMessage(txt,
                    "Ариана Гранде", "date_grande",
                    "Марго Робби", "date_robbie",
                    "Зендея", "date_zendaya",
                    "Райан Гослинг", "date_gosling",
                    "Том Харди", "date_hardy"
                    );
            return;
        }

        if (currentMode == DialogMode.DATE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")) {
                sendPhotoMessage(query);
                sendTextMessage("Отличный выбор!");

                String prompt = loadPrompt(query);
                chatGPT.setPrompt(prompt);
                return;
            }

            Message msg = sendTextMessage("Please, wait...");
            String answer = chatGPT.addMessage(message);
            updateTextMessage(msg, answer);
            return;
        }

        // command MESSAGE
        if (message.equals("/message")) {
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage("Пришлите в чат вашу переписку",
                    "Следующее сообщение", "message_next",
                    "Пригласить на свидание", "message_date"
                    );
            return;
        }

        if (currentMode == DialogMode.MESSAGE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")) {
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n", list);

                Message msg = sendTextMessage("Please, wait...");
                String answer = chatGPT.sendMessage(prompt, userChatHistory);
                updateTextMessage(msg, answer);
                return;
            }
            list.add(message);
            return;
        }

        // command PROFILE
        if (message.equals("/profile")) {
            currentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");

            me = new UserInfo();
            questionCount = 1;
            sendTextMessage("Сколько вам лет?");
            return;
        }

        if (currentMode == DialogMode.PROFILE && !isMessageCommand()) {

            switch (questionCount) {
                case 1:
                    me.age = message;

                    questionCount = 2;
                    sendTextMessage("Кем вы работаете?");
                    return;
                case 2:
                    me.occupation = message;

                    questionCount = 3;
                    sendTextMessage("Какое у вас хобби?");
                    return;
                case 3:
                    me.hobby = message;

                    questionCount = 4;
                    sendTextMessage("Что вам не нравится?");
                    return;
                case 4:
                    me.annoys = message;

                    questionCount = 5;
                    sendTextMessage("Что вы ждете от знакомства?");
                    return;
                case 5:
                    me.goals = message;

                    String aboutMySelf = me.toString();
                    String prompt = loadPrompt("profile");
                    Message msg = sendTextMessage("Please, wait...");
                    String answer = chatGPT.sendMessage(prompt, aboutMySelf);
                    updateTextMessage(msg, answer);
                    return;
            }

            return;
        }

        // command OPENER
        if (message.equals("/opener")) {
            currentMode = DialogMode.OPENER;

            she = new UserInfo();
            questionCount = 1;
            sendPhotoMessage("opener");
            sendTextMessage("Имя девушки");
            return;
        }

        if (currentMode == DialogMode.OPENER && !isMessageCommand()) {

            switch (questionCount) {
                case 1:
                    she.name = message;

                    questionCount = 2;
                    sendTextMessage("Сколько ей лет?");
                    return;
                case 2:
                    she.age = message;

                    questionCount = 3;
                    sendTextMessage("Какое у нее хобби?");
                    return;
                case 3:
                    she.hobby = message;

                    questionCount = 4;
                    sendTextMessage("Кем она работает?");
                    return;
                case 4:
                    she.occupation = message;

                    questionCount = 5;
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    she.goals = message;

                    String aboutFriend = she.toString();
                    String prompt = loadPrompt("opener");
                    Message msg = sendTextMessage("Please, wait...");
                    String answer = chatGPT.sendMessage(prompt, aboutFriend);
                    updateTextMessage(msg, answer);
                    return;
            }

            return;
        }

        sendTextMessage("*Hello*, _User!_");
        sendTextMessage("Your text message: " + message);
        sendTextButtonsMessage("Choose the button: ",
                "Start", "start",
                "Stop", "stop");

    }



    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
