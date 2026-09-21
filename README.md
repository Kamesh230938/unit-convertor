# Unit Converter — Simple Java Edition

A beginner-friendly full-stack Unit Converter using only:

- HTML5
- CSS3
- JavaScript ES6+
- Core Java

No Maven. No Spring Boot. No external libraries.

## Features

- Length
- Weight
- Temperature
- Area
- Volume
- Speed
- Time
- Real-time conversion
- Swap units
- Copy result
- Recent history
- Dark/light mode
- Responsive UI
- Java backend using the built-in `HttpServer`
- JSON API without external libraries

## Requirements

Java JDK 17 or newer.

## Project structure

```text
unit-converter-simple/
├── README.md
├── .gitignore
├── run.bat
├── run.sh
├── src/
│   └── UnitConverterServer.java
└── web/
    ├── index.html
    ├── styles.css
    └── app.js
```

## Run on Windows

Double-click `run.bat`, or open Command Prompt in the project folder:

```bat
javac -d out src\UnitConverterServer.java
java -cp out UnitConverterServer
```

Then open:

```text
http://localhost:8080
```

## Run on Linux/macOS

```bash
chmod +x run.sh
./run.sh
```

Then open:

```text
http://localhost:8080
```

## API

```text
GET /api/categories
```

```text
POST /api/convert
```

Example:

```json
{
  "category": "length",
  "from": "meter",
  "to": "kilometer",
  "value": 1500
}
```

The Java server serves the frontend and handles conversion requests. Conversion history and theme preference are stored in the browser's localStorage.

## Stop the server

Press `Ctrl + C` in the terminal.
