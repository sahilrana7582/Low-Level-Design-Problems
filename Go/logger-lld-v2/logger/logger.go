package logger

import (
	"fmt"
	"os"
	"path/filepath"
	"time"
)

const outDir = "logs"

type LogLevel int

const (
	DEBUG LogLevel = iota
	INFO
	WARN
	ERROR
)

// String lets fmt print a LogLevel as its name, so "INFO" is written in one place only.
func (level LogLevel) String() string {
	switch level {
	case DEBUG:
		return "DEBUG"
	case INFO:
		return "INFO"
	case WARN:
		return "WARN"
	case ERROR:
		return "ERROR"
	default:
		return "UNKNOWN"
	}
}

type Destination int

const (
	CONSOLE Destination = iota
	FILE
)

type Logger interface {
	Debug(message string)
	Info(message string)
	Warn(message string)
	Error(message string)
	Level() LogLevel
	Close() error
}

type logger struct {
	level LogLevel
	out   *os.File // os.Stdout for CONSOLE, today's log file for FILE
}

// New checks its inputs before touching the disk, and returns an error instead of exiting:
// the caller decides what a broken logger means for their program.
func New(level LogLevel, dest Destination) (Logger, error) {
	if level < DEBUG || level > ERROR {
		return nil, fmt.Errorf("invalid log level: %d", level)
	}

	out, err := openDestination(dest)
	if err != nil {
		return nil, err
	}

	return &logger{level: level, out: out}, nil
}

func openDestination(dest Destination) (*os.File, error) {
	switch dest {
	case CONSOLE:
		return os.Stdout, nil
	case FILE:
		if err := os.MkdirAll(outDir, 0755); err != nil {
			return nil, fmt.Errorf("creating log directory: %w", err)
		}
		path := filepath.Join(outDir, time.Now().Format(time.DateOnly)+".log")
		file, err := os.OpenFile(path, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0644)
		if err != nil {
			return nil, fmt.Errorf("opening log file: %w", err)
		}
		return file, nil
	default:
		return nil, fmt.Errorf("invalid destination: %d", dest)
	}
}

func (l *logger) Debug(message string) { l.write(DEBUG, message) }
func (l *logger) Info(message string)  { l.write(INFO, message) }
func (l *logger) Warn(message string)  { l.write(WARN, message) }
func (l *logger) Error(message string) { l.write(ERROR, message) }

func (l *logger) Level() LogLevel { return l.level }

// write is the only place that filters, formats and writes.
// The four methods above just name their level, so they cannot drift apart.
func (l *logger) write(level LogLevel, message string) {
	if level < l.level {
		return
	}
	fmt.Fprintf(l.out, "%s %s %s\n", time.Now().Format(time.RFC3339), level, message)
}

// Close releases the log file. The console is shared with the rest of the program, so it is left open.
func (l *logger) Close() error {
	if l.out == os.Stdout {
		return nil
	}
	return l.out.Close()
}
