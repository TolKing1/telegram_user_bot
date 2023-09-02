# Java Telegram User Bot

This Java Telegram User Bot is a versatile and customizable bot designed for use with the Telegram messaging platform. It offers features like responding to specific commands, handling YouTube video links, animating text, and more.

## Getting Started

1. **Obtain API Credentials**: To use this bot, you'll need to obtain your API credentials (API ID and API Hash) from [my.telegram.org](https://my.telegram.org/). Replace these values in the `config.properties` file.

2. **Install Dependencies**: This bot relies on the TDLib library for Telegram API communication. You can find TDLib installation instructions [here](https://tdlib.github.io/td/build.html).

3. **Compile and Run**: Compile and run the `Bot.java` file in your Java environment. The bot will initialize and connect to Telegram.

## Features

- **YouTube Link Handling**: The bot can process YouTube links and send video messages.
- **Custom Commands**: Customize commands to perform various actions within your chat.
- **Text Animation**: Animate text by sending messages starting with '$'.
- **Text Recognition**: Get text of the image (only ENG).

## Usage

- To stop the bot, send the `/stop` command (only allowed for administrators) in telegram.
- To animate text, start your message with '$' in telegram.
- To download youtube videos just paste link in telegram.
- To get text of the image by sending "check" with replay to wanted image

## Configuration

- API credentials (API ID and API Hash), phone number, and user/administrator IDs can be configured in the `config.properties` file.
- API keys for YouTube and OCR (check TODO list)

## Contributing

Contributions to this project are welcome! Feel free to submit issues, pull requests, or feature suggestions.

## Acknowledgments

- This bot uses the TDLib library for Telegram API communication.

---

**Disclaimer:** Please use this bot responsibly and in compliance with Telegram's terms of service and guidelines.
