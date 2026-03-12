package com.ashes.dev.works.ai.neural.brain.medha.domain.model

data class PromptTemplate(
    val icon: String,
    val title: String,
    val description: String,
    val promptPrefix: String,
    val requiresInput: Boolean = true,
    val category: TemplateCategory
)

enum class TemplateCategory {
    WRITING, ANALYSIS, CODE, CREATIVE, UTILITY, IMAGE
}

object PromptTemplates {
    val all = listOf(
        // Analysis
        PromptTemplate(
            icon = "\uD83D\uDCDD",
            title = "Summarize Text",
            description = "Condense text into key points",
            promptPrefix = "Summarize the following text concisely, highlighting the key points:\n\n",
            category = TemplateCategory.ANALYSIS
        ),
        PromptTemplate(
            icon = "\uD83D\uDD0D",
            title = "Explain Simply",
            description = "ELI5 - Explain like I'm 5",
            promptPrefix = "Explain the following in very simple terms that a 5 year old could understand:\n\n",
            category = TemplateCategory.ANALYSIS
        ),
        PromptTemplate(
            icon = "\u2696\uFE0F",
            title = "Pros & Cons",
            description = "Analyze advantages and disadvantages",
            promptPrefix = "List the pros and cons of the following in a clear table format:\n\n",
            category = TemplateCategory.ANALYSIS
        ),
        PromptTemplate(
            icon = "\uD83D\uDCA1",
            title = "Key Takeaways",
            description = "Extract main insights",
            promptPrefix = "Extract the key takeaways and main insights from the following:\n\n",
            category = TemplateCategory.ANALYSIS
        ),

        // Writing
        PromptTemplate(
            icon = "\u2709\uFE0F",
            title = "Write Email",
            description = "Draft a professional email",
            promptPrefix = "Write a professional email about the following:\n\n",
            category = TemplateCategory.WRITING
        ),
        PromptTemplate(
            icon = "\uD83D\uDCDA",
            title = "Fix Grammar",
            description = "Correct grammar and improve clarity",
            promptPrefix = "Fix the grammar, spelling, and improve the clarity of the following text. Show the corrected version:\n\n",
            category = TemplateCategory.WRITING
        ),
        PromptTemplate(
            icon = "\uD83C\uDF10",
            title = "Translate",
            description = "Translate text to another language",
            promptPrefix = "Translate the following text. If no target language is specified, translate to English:\n\n",
            category = TemplateCategory.WRITING
        ),
        PromptTemplate(
            icon = "\uD83D\uDCC4",
            title = "Rewrite Formal",
            description = "Make text more professional",
            promptPrefix = "Rewrite the following text in a more formal and professional tone:\n\n",
            category = TemplateCategory.WRITING
        ),

        // Code
        PromptTemplate(
            icon = "\uD83D\uDCBB",
            title = "Explain Code",
            description = "Break down code step by step",
            promptPrefix = "Explain the following code step by step, describing what each part does:\n\n",
            category = TemplateCategory.CODE
        ),
        PromptTemplate(
            icon = "\uD83D\uDC1B",
            title = "Debug Code",
            description = "Find and fix bugs in code",
            promptPrefix = "Analyze the following code for bugs, issues, and suggest fixes:\n\n",
            category = TemplateCategory.CODE
        ),
        PromptTemplate(
            icon = "\u267B\uFE0F",
            title = "Refactor Code",
            description = "Improve code quality",
            promptPrefix = "Refactor the following code for better readability, performance, and best practices:\n\n",
            category = TemplateCategory.CODE
        ),
        PromptTemplate(
            icon = "\uD83D\uDCDD",
            title = "Write Code",
            description = "Generate code from description",
            promptPrefix = "Write clean, well-commented code for the following requirement:\n\n",
            category = TemplateCategory.CODE
        ),

        // Creative
        PromptTemplate(
            icon = "\uD83D\uDCA1",
            title = "Brainstorm Ideas",
            description = "Generate creative ideas",
            promptPrefix = "Brainstorm 10 creative and unique ideas for the following:\n\n",
            category = TemplateCategory.CREATIVE
        ),
        PromptTemplate(
            icon = "\uD83D\uDCD6",
            title = "Write Story",
            description = "Create a short story",
            promptPrefix = "Write a creative short story based on the following premise:\n\n",
            category = TemplateCategory.CREATIVE
        ),
        PromptTemplate(
            icon = "\uD83C\uDFAD",
            title = "Write Poem",
            description = "Compose a poem",
            promptPrefix = "Write a poem about the following topic:\n\n",
            category = TemplateCategory.CREATIVE
        ),

        // Utility
        PromptTemplate(
            icon = "\uD83D\uDCCA",
            title = "Compare",
            description = "Compare two or more things",
            promptPrefix = "Compare the following items in detail, covering key differences and similarities:\n\n",
            category = TemplateCategory.UTILITY
        ),
        PromptTemplate(
            icon = "\uD83D\uDCC5",
            title = "Make Plan",
            description = "Create an action plan",
            promptPrefix = "Create a detailed step-by-step action plan for the following goal:\n\n",
            category = TemplateCategory.UTILITY
        ),
        PromptTemplate(
            icon = "\u2753",
            title = "Quiz Me",
            description = "Generate quiz questions on a topic",
            promptPrefix = "Create 5 quiz questions (with answers) about the following topic:\n\n",
            category = TemplateCategory.UTILITY
        ),
        PromptTemplate(
            icon = "\uD83D\uDCCB",
            title = "Make List",
            description = "Create an organized list",
            promptPrefix = "Create a well-organized list for the following:\n\n",
            category = TemplateCategory.UTILITY
        ),

        // Image
        PromptTemplate(
            icon = "\uD83D\uDDBC\uFE0F",
            title = "Analyze Image",
            description = "Describe and analyze an image",
            promptPrefix = "Analyze this image in detail. Describe what you see, the context, and any notable elements.",
            requiresInput = false,
            category = TemplateCategory.IMAGE
        ),
        PromptTemplate(
            icon = "\uD83D\uDCF7",
            title = "Extract Text from Image",
            description = "OCR - Read text in an image",
            promptPrefix = "Extract and transcribe all visible text from this image. Format it neatly.",
            requiresInput = false,
            category = TemplateCategory.IMAGE
        ),
        PromptTemplate(
            icon = "\uD83C\uDFA8",
            title = "Describe Art Style",
            description = "Analyze the artistic style",
            promptPrefix = "Analyze the artistic style, techniques, colors, and composition of this image.",
            requiresInput = false,
            category = TemplateCategory.IMAGE
        )
    )
}
