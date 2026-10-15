package logger

import (
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"
	"time"
)

type LogLevel int

const outDir = "logs"

const (
	DEBUG LogLevel = 0
	INFO  LogLevel = 1
	WARN  LogLevel = 2
	ERROR LogLevel = 3
)

type destination int

const (
	CONSOLE destination = 0
	FILE    destination = 1
)

type logger struct {
	logLevel LogLevel
	file     *os.File
}

type Logger interface {
	GetLogLevel() string
	Info(message string)
	Debug(message string)
	Warn(message string)
	Error(message string)
}

func (logger logger) GetLogLevel() string {
	switch logger.logLevel {
	case 0:
		return "DEBUG"
	case 1:
		return "INFO"
	case 2:
		return "WARN"
	default:
		return "ERROR"
	}
}

func (logger logger) Info(message string) {
	if !permitLog(logger.logLevel, INFO) {
		return
	}
	strData := fmt.Sprintf("%s %s %s\n", time.Now().Format(time.RFC3339), "INFO", message)
	logData := []byte(strData)
	logger.file.Write(logData)
}

func (logger logger) Debug(message string) {
	if !permitLog(logger.logLevel, DEBUG) {
		return
	}
	fmt.Printf("%s %s %s\n", time.Now().Format(time.RFC3339), "DEBUG", message)
}

func (logger logger) Warn(message string) {
	if !permitLog(logger.logLevel, DEBUG) {
		return
	}
	fmt.Printf("%s %s %s\n", time.Now().Format(time.RFC3339), "WARN", message)
}

func (logger logger) Error(message string) {
	if !permitLog(logger.logLevel, DEBUG) {
		return
	}
	fmt.Printf("%s %s %s\n", time.Now().Format(time.RFC3339), "ERROR", message)
}

func New(level string) *logger {
	level = strings.TrimSpace(level)

	if level == "" {
		log.Fatal("Empty Log Level")
	}

	// 1. Ensure the output directory exists.
	if err := os.MkdirAll(outDir, 0755); err != nil {
		log.Fatalf("Error creating log directory: %v", err)
	}

	// 2. Build today's log file path.
	todayDate := time.Now().Format(time.DateOnly)
	path := filepath.Join(outDir, todayDate+".log")

	// 3. Create the file if missing; otherwise append to it.
	file, err := os.OpenFile(
		path,
		os.O_CREATE|os.O_APPEND|os.O_WRONLY,
		0644,
	)
	if err != nil {
		log.Fatalf("Error opening log file: %v", err)
	}

	// 4. Parse the log level.
	var logLevel LogLevel

	switch strings.ToLower(level) {
	case "debug":
		logLevel = DEBUG
	case "info":
		logLevel = INFO
	case "warn":
		logLevel = WARN
	case "error":
		logLevel = ERROR
	case "fatal":
	default:
		log.Fatalf("Invalid log level: %s", level)
	}

	return &logger{
		logLevel: logLevel,
		file:     file,
	}
}

func permitLog(logLevel, callingFrom LogLevel) bool {
	return logLevel <= callingFrom
}
