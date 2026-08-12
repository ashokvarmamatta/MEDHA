package com.ashes.dev.works.ai.neural.brain.medha.ui.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The handful of Material icons MEDHA uses that are not in `material-icons-core`.
 *
 * **Why hand-author three vectors instead of depending on `material-icons-extended`:** that
 * library declares every Material icon as its own lazily-built property, which lands in the APK
 * as ~32 MB of dex (`classes13/14/15.dex`). R8 strips the unused ones in a release build, but
 * MEDHA's GitHub workflow ships the DEBUG apk, where nothing is shrunk — so users were carrying
 * thousands of icons to get three. A full audit of `Icons.*` usage found 19 icons in total, 16 of
 * which core already provides.
 *
 * Path data is the standard 24dp Material filled geometry, so these render identically to the
 * library versions. If you need a fourth icon, check core first; only add here if it is missing.
 */
object MedhaIcons {

    /** Two overlapping sheets — the "copy message" action. */
    val ContentCopy: ImageVector by lazy {
        materialIcon(name = "Medha.ContentCopy") {
            materialPath {
                moveTo(16.0f, 1.0f)
                horizontalLineTo(4.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(14.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(3.0f)
                horizontalLineToRelative(12.0f)
                verticalLineTo(1.0f)
                close()
                moveTo(19.0f, 5.0f)
                horizontalLineTo(8.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(11.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(7.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(19.0f, 21.0f)
                horizontalLineTo(8.0f)
                verticalLineTo(7.0f)
                horizontalLineToRelative(11.0f)
                verticalLineToRelative(14.0f)
                close()
            }
        }
    }

    /** Classic folder — the shared model folder row in Settings. */
    val Folder: ImageVector by lazy {
        materialIcon(name = "Medha.Folder") {
            materialPath {
                moveTo(10.0f, 4.0f)
                horizontalLineTo(4.0f)
                curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                lineTo(2.0f, 18.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(16.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(8.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                horizontalLineToRelative(-8.0f)
                lineToRelative(-2.0f, -2.0f)
                close()
            }
        }
    }

    /** Filled square — stop an in-flight generation. */
    val Stop: ImageVector by lazy {
        materialIcon(name = "Medha.Stop") {
            materialPath {
                moveTo(6.0f, 6.0f)
                horizontalLineToRelative(12.0f)
                verticalLineToRelative(12.0f)
                horizontalLineTo(6.0f)
                close()
            }
        }
    }
}
