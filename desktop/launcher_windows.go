// Windows GUI launcher for the locally installed Java runtime.
package main

import (
	"os"
	"os/exec"
	"path/filepath"
	"syscall"
	"unsafe"
)

func alert(message string) {
	title, _ := syscall.UTF16PtrFromString("Dispensary Desktop")
	text, _ := syscall.UTF16PtrFromString(message)
	syscall.NewLazyDLL("user32.dll").NewProc("MessageBoxW").Call(0,
		uintptr(unsafe.Pointer(text)), uintptr(unsafe.Pointer(title)), 0x10)
}

func main() {
	executable, err := os.Executable()
	if err != nil {
		alert(err.Error())
		return
	}
	jar := filepath.Join(filepath.Dir(executable), "Dispensary.jar")
	if _, err := os.Stat(jar); err != nil {
		alert("Extract the whole ZIP before opening Dispensary.exe. Keep Dispensary.jar beside it.")
		return
	}
	var java string
	if home := os.Getenv("JAVA_HOME"); home != "" {
		candidate := filepath.Join(home, "bin", "javaw.exe")
		if _, err := os.Stat(candidate); err == nil {
			java = candidate
		}
	}
	if java == "" {
		java, err = exec.LookPath("javaw.exe")
		if err != nil {
			alert("Install Java 17 or newer and set JAVA_HOME, then reopen Dispensary.")
			return
		}
	}
	if err := exec.Command(java, "-jar", jar, "--desktop").Run(); err != nil {
		alert("Dispensary could not start. Check that Java 17 or newer is installed.")
	}
}
