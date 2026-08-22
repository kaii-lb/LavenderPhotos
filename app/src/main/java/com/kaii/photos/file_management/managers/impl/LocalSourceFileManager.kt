package com.kaii.photos.file_management.managers.impl

import com.kaii.photos.file_management.managers.traits.Copy
import com.kaii.photos.file_management.managers.traits.CountAndSize
import com.kaii.photos.file_management.managers.traits.Delete
import com.kaii.photos.file_management.managers.traits.ExtractExif
import com.kaii.photos.file_management.managers.traits.Favourite
import com.kaii.photos.file_management.managers.traits.Move
import com.kaii.photos.file_management.managers.traits.PrepareFileForWrite
import com.kaii.photos.file_management.managers.traits.RenameAlbum
import com.kaii.photos.file_management.managers.traits.RenameFile
import com.kaii.photos.file_management.managers.traits.Secure
import com.kaii.photos.file_management.managers.traits.Share
import com.kaii.photos.file_management.managers.traits.Trash

interface LocalSourceFileManager : Copy, Move, RenameFile, RenameAlbum, Trash, Delete, Secure, Share, Favourite, ExtractExif, CountAndSize, PrepareFileForWrite