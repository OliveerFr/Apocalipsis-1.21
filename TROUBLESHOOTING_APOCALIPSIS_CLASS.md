# Troubleshooting: "cannot access Apocalipsis" Error

## Error Description
```
cannot access Apocalipsis
bad source file: Apocalipsis.java
file does not contain class me.apocalipsis.Apocalipsis
Please remove or make sure it appears in the correct subdirectory of the sourcepath.
(errors(1): 8:1-8:35)
```

## Root Cause Analysis

The error message references line 8 of `Apocalipsis.java`, which contains the package declaration:
```java
package me.apocalipsis;
```

This error typically occurs when:
1. **Maven/IDE cache is stale** - Old compilation artifacts interfere with new builds
2. **Dependencies are missing** - Paper API (Bukkit) dependency hasn't been downloaded
3. **Network issues** - Maven cannot reach repository to download Paper API
4. **IDE out of sync** - IDE's project model doesn't match the actual codebase

## Verification

The code structure has been verified as correct:
- ✅ File location: `src/main/java/me/apocalipsis/Apocalipsis.java`
- ✅ Package declaration: `package me.apocalipsis;` (line 8)
- ✅ Class declaration: `public final class Apocalipsis extends JavaPlugin` (line 39)
- ✅ File encoding: UTF-8
- ✅ No BOM (Byte Order Mark) present
- ✅ No circular imports
- ✅ No duplicate files

## Solution Steps

### 1. Clean Maven Build
```bash
mvn clean
```

### 2. Force Update Dependencies
```bash
mvn clean install -U
```
The `-U` flag forces Maven to update snapshots and releases from remote repositories.

### 3. Clear Maven Cache (if step 2 doesn't work)
```bash
# On Windows
rmdir /s /q %USERPROFILE%\.m2\repository\io\papermc

# On Linux/Mac
rm -rf ~/.m2/repository/io/papermc
```
Then run:
```bash
mvn clean install
```

### 4. IDE-Specific Solutions

#### IntelliJ IDEA
1. File → Invalidate Caches / Restart
2. Right-click on project → Maven → Reload Project
3. Build → Rebuild Project

#### Eclipse
1. Right-click on project → Maven → Update Project → Force Update
2. Project → Clean → Clean all projects

#### VS Code
1. Run command: "Java: Clean Java Language Server Workspace"
2. Reload window

### 5. Verify Java Version
The project requires Java 21. Verify your Java version:
```bash
java -version
javac -version
```

If you're not using Java 21, download it from:
- [Adoptium Temurin 21](https://adoptium.net/temurin/releases/?version=21)

### 6. Verify Maven Configuration
Ensure your `pom.xml` has the correct repository configuration:
```xml
<repositories>
    <repository>
        <id>papermc-repo</id>
        <url>https://repo.papermc.io/repository/maven-public/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.21.8-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### 7. Check Internet Connection
Verify you can reach the Paper MC repository:
```bash
# Windows
ping repo.papermc.io

# Linux/Mac
curl -I https://repo.papermc.io/repository/maven-public/
```

### 8. Alternative: Manual Dependency Installation
If you cannot access the repository directly, download the Paper API manually:
1. Download from: https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21.8-R0.1-SNAPSHOT/
2. Install locally:
```bash
mvn install:install-file \
  -Dfile=paper-api-1.21.8-R0.1-SNAPSHOT.jar \
  -DgroupId=io.papermc.paper \
  -DartifactId=paper-api \
  -Dversion=1.21.8-R0.1-SNAPSHOT \
  -Dpackaging=jar
```

## Common Mistakes to Avoid

1. **Don't modify Apocalipsis.java** - The file is correctly structured
2. **Don't change the package declaration** - It must match the directory structure
3. **Don't skip the clean step** - Old artifacts can cause conflicts
4. **Check your firewall** - Corporate firewalls may block Maven repositories

## Verification After Fix

After applying the solution, verify the fix:
```bash
mvn clean compile
```

You should see:
```
[INFO] BUILD SUCCESS
```

## Still Having Issues?

If the error persists:
1. Check for antivirus interference with Maven downloads
2. Try using a VPN if your network blocks Maven repositories
3. Verify you have write permissions to the `.m2` directory
4. Check for proxy configuration in Maven settings (`~/.m2/settings.xml`)
5. Ensure no other IDE or process is locking the project files

## Related Files
- `pom.xml` - Maven configuration
- `src/main/java/me/apocalipsis/Apocalipsis.java` - Main plugin class
- `.gitattributes` - Git line ending configuration

## Summary
The "cannot access Apocalipsis" error is almost always caused by missing or corrupted Maven dependencies. The solution is to clean the build, force update dependencies, and ensure Maven can access the Paper MC repository. The Apocalipsis.java source file itself is correct and does not need modification.
