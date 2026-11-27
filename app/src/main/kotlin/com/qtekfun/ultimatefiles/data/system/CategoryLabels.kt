package com.qtekfun.ultimatefiles.data.system

import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.domain.usecase.Category

fun Category.labelRes(): Int = when (this) {
    Category.IMAGES -> R.string.category_images
    Category.VIDEO -> R.string.category_video
    Category.AUDIO -> R.string.category_audio
    Category.DOCUMENTS -> R.string.category_documents
    Category.APKS -> R.string.category_apks
    Category.RECENTS -> R.string.category_recents
}
