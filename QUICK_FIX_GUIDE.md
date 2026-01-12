# Quick Fix Guide - "Cannot Access Apocalipsis" Error

## 🚨 Error Symptoms
- Build fails with "cannot access Apocalipsis"
- IDE shows red underlines on Apocalipsis class
- Maven compile error referencing line 8 of Apocalipsis.java

## ✅ Quick Solutions (Try in Order)

### Solution 1: Clean and Rebuild (90% success rate)
```bash
# Command line
mvn clean install -U

# Or use the enhanced build script
./build-enhanced.sh        # Linux/Mac
build-enhanced.bat         # Windows
```

### Solution 2: Force Update Dependencies
```bash
mvn clean
mvn dependency:purge-local-repository
mvn install
```

### Solution 3: Clear Maven Cache
```bash
# Windows
rmdir /s /q %USERPROFILE%\.m2\repository\io\papermc
mvn clean install

# Linux/Mac
rm -rf ~/.m2/repository/io/papermc
mvn clean install
```

### Solution 4: IDE Refresh
**IntelliJ IDEA:**
- File → Invalidate Caches / Restart
- Right-click project → Maven → Reload Project

**Eclipse:**
- Right-click project → Maven → Update Project (Force Update)

**VS Code:**
- Cmd/Ctrl + Shift + P → "Java: Clean Java Language Server Workspace"

### Solution 5: Verify Java Version
```bash
java -version    # Should show version 21
javac -version   # Should show version 21
```
If not Java 21, download from: https://adoptium.net/temurin/releases/?version=21

### Solution 6: Check Network
```bash
# Verify access to Paper MC repository
ping repo.papermc.io
```

## 🔍 Root Cause
This error occurs when:
1. Maven dependencies (Paper API) are not downloaded
2. Maven/IDE cache is stale or corrupted
3. Network cannot reach Paper MC repository
4. Wrong Java version is being used

## 📋 What NOT to Do
- ❌ Don't modify Apocalipsis.java (it's correct)
- ❌ Don't change the package declaration
- ❌ Don't delete the src directory
- ❌ Don't change the pom.xml dependencies (unless updating versions)

## 📚 Detailed Documentation
For more information, see: `TROUBLESHOOTING_APOCALIPSIS_CLASS.md`

## 💡 Prevention
- Keep Maven cache clean: `mvn clean` before each build
- Use `-U` flag to force update: `mvn clean install -U`
- Ensure stable internet connection when building
- Use Java 21 consistently across all tools

## 🆘 Still Stuck?
1. Run with debug output: `mvn clean compile -X > build-log.txt`
2. Check the log for specific errors
3. Verify firewall/antivirus isn't blocking Maven
4. Try building from command line instead of IDE
5. Check proxy settings in `~/.m2/settings.xml`

## ✨ Success Indicators
After applying the fix, you should see:
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX s
```

And the JAR file will be created:
```
target/Apocalipsis-1.0.1.jar
```
