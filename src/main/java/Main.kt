import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import java.io.*
import java.util.*
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by david on 7/16/17.
 */

val FEMALE_NAMES_FILE = "female_names"
val MALE_NAMES_FILE = "male_names"
val LAST_NAMES_FILE = "last_names"

private val <T> Array<T>.random: T
    get() = this[ThreadLocalRandom.current().nextInt(this.size)]

class FileLoader(fileName: String) {
    val file: File = File(javaClass.classLoader.getResource(fileName).toURI())

    fun getInputStream(): InputStream = FileInputStream(file)
}

data class Credentials(var name: String, var lastName: String, var email: String, var password: Array<Char>)

class GenerationException(msg: String) : Throwable(msg)

fun main(args: Array<String>) {
    println("-- What gender do you want to generate? (M/F)")
    print("-> ")
    val gender = readLine()

    when (gender) {
        "m", "M" -> generateWithFile(MALE_NAMES_FILE)
        "f", "F" -> generateWithFile(FEMALE_NAMES_FILE)
        else -> println("Wrong input.")
    }
}

fun generateWithFile(fileName: String) {
    Observable
            .zip(generateRandomEntry(fileName), generateRandomEntry(LAST_NAMES_FILE), BiFunction<String, String, Credentials>
            { name, lastName ->
                Credentials(name, lastName, "", arrayOf())
            })
            .map {
                credentials ->
                credentials.apply {
                    email = generateEmail(name, lastName)
                    password = generatePassword()
                }
            }
            .singleOrError()
            .subscribe({
                credentials ->
                printCredentials(credentials)
            }, {
                error ->
                println("-- Error occurred\n-- ${error.message}.")
            })
}

fun generateRandomEntry(fileName: String): Observable<String> = Observable.fromCallable {
    val loader = FileLoader(fileName)

    if (loader.file.isFile) {
        val size = loader.file.length()
        val reader = BufferedReader(InputStreamReader(loader.getInputStream()))
        val random = ThreadLocalRandom.current().nextLong(size)

        reader.skip(random)
        var line = ""

        do {
            try {
                line = reader.readLine()
            } catch (eof: EOFException) {
                reader.reset()
            }
        } while (line.isEmpty() || Character.isLowerCase(line[0]))

        reader.close()
        line
    } else {
        throw GenerationException("There is no such file: ${loader.file.absolutePath}.")
    }
}

fun generateEmail(name: String, surname: String): String {
    val random = Random()
    val email = StringBuilder()
    val separators = arrayOf('.', '_', '-')

    if (random.nextBoolean()) {
        email.append(name)
        email.append(separators.random)
        email.append(surname)
    } else {
        email.append(surname)
        email.append(separators.random)
        email.append(name)
    }

    if (random.nextBoolean()) {
        email.append(separators.random)
        email.append(random.nextInt(100000))
    }

    email.append("@gmail.com")

    return email.toString().toLowerCase()
}

fun generatePassword(): Array<Char> {
    val size = ThreadLocalRandom.current().nextInt(8, 16)
    val chars = arrayOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
            'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'r', 's', 't', 'w', 'y', 'z', 'x', 'v', '.', '_', '!', 'q')
    return Array(size, {
        if (ThreadLocalRandom.current().nextBoolean())
            chars.random.toUpperCase()
        else
            chars.random
    })
}

fun printCredentials(credentials: Credentials) {
    credentials.apply {
        println("-- Name: $name")
        println("-- Surname: $lastName")
        println("-- E-mail: $email")
        print("-- Password: ")
        password.forEach { print(it) }.also { print("\n") }
    }
}