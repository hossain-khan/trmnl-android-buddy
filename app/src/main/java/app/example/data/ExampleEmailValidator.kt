package app.example.data

// Example of a class participates in Metro dependency injection.
class ExampleEmailValidator {
    fun isValidEmail(email: String): Boolean = email.contains("@")
}
