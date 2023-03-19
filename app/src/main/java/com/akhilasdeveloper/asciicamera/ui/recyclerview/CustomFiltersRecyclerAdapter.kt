package com.akhilasdeveloper.asciicamera.ui.recyclerview

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.akhilasdeveloper.asciicamera.databinding.CustomFilterListItemBinding
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters.Companion.FilterSpecs
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CustomFiltersRecyclerAdapter(
    private val interaction: RecyclerCustomFiltersClickListener? = null,
    private val sampleBitmap: Bitmap,
    private val scope: CoroutineScope,
    ) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val bindingPhoto = CustomFilterListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PhotoViewHolder(bindingPhoto, interaction, sampleBitmap, scope)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val currentItem = differ.currentList[position]

            val photoItemViewHolder = holder as PhotoViewHolder
            currentItem?.let {
                photoItemViewHolder.bindPhoto(currentItem, position)
            }
        }


        class PhotoViewHolder(
            private val binding: CustomFilterListItemBinding,
            private val interaction: RecyclerCustomFiltersClickListener?,
            private val sampleBitmap: Bitmap,
            private val scope: CoroutineScope
        ) :
            RecyclerView.ViewHolder(binding.root) {

            fun bindPhoto(photo: FilterSpecs, position: Int) {

                val asciiGenerator = AsciiGenerator()

                asciiGenerator.fgColor = photo.fgColor
                asciiGenerator.bgColor = photo.bgColor
                asciiGenerator.density = photo.density
                asciiGenerator.densityByteArray = photo.densityArray
                asciiGenerator.colorType = photo.fgColorType
                asciiGenerator.name = photo.name

                scope.launch {
                    binding.filterItemImage.setImageBitmap(asciiGenerator.imageBitmapToTextBitmap(sampleBitmap))
                }

                binding.root.setOnClickListener {
                    interaction?.onCustomItemClicked(photo)
                }

                binding.filterName.text = photo.name

                binding.deleteButton.setOnClickListener {
                    interaction?.onCustomDeleteClicked(photo)
                }
            }

        }

        fun submitList(list: List<FilterSpecs>) {
            differ.submitList(list)
        }

        private val differ = AsyncListDiffer(
            RoverRecyclerChangeCallback(this),
            AsyncDifferConfig.Builder(DataDiffUtil).build()
        )

        internal inner class RoverRecyclerChangeCallback(
            private val adapter: CustomFiltersRecyclerAdapter
        ) : ListUpdateCallback {

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                adapter.notifyItemChanged(position)
            }

            override fun onInserted(position: Int, count: Int) {
                adapter.notifyItemInserted(position)
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                adapter.notifyItemMoved(fromPosition, toPosition)
            }

            override fun onRemoved(position: Int, count: Int) {
                adapter.notifyItemRemoved(position)
            }
        }

        companion object {
            private val DataDiffUtil = object : DiffUtil.ItemCallback<FilterSpecs>() {
                override fun areItemsTheSame(
                    oldItem: FilterSpecs,
                    newItem: FilterSpecs
                ) =
                    oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: FilterSpecs,
                    newItem: FilterSpecs
                ) =
                    oldItem == newItem

            }
        }

        override fun getItemCount(): Int {
            return differ.currentList.size
        }
    }