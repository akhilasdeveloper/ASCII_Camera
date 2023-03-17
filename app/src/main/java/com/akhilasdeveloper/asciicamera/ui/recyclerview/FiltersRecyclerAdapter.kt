package com.akhilasdeveloper.asciicamera.ui.recyclerview

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.akhilasdeveloper.asciicamera.databinding.FilterListItemBinding
import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter

class FiltersRecyclerAdapter(
        private val interaction: RecyclerFiltersClickListener? = null,
        private val sampleBitmap: Bitmap,
    ) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val bindingPhoto = FilterListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PhotoViewHolder(bindingPhoto, interaction, sampleBitmap)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val currentItem = differ.currentList[position]

            val photoItemViewHolder = holder as PhotoViewHolder
            currentItem?.let {
                photoItemViewHolder.bindPhoto(currentItem, position)
            }
        }


        class PhotoViewHolder(
            private val binding: FilterListItemBinding,
            private val interaction: RecyclerFiltersClickListener?,
            private val sampleBitmap: Bitmap
        ) :
            RecyclerView.ViewHolder(binding.root) {

            fun bindPhoto(photo: TextBitmapFilter, position: Int) {
                /*binding.filterItemImage.filter = photo
                binding.filterItemImage.generateTextViewFromBitmap(bitmap = sampleBitmap)
                binding.filterName.text = photo.name
                binding.root.setOnClickListener {
                    interaction?.onItemClicked(photo)
                }*/
            }

        }

        fun submitList(list: List<TextBitmapFilter>) {
            differ.submitList(list)
        }

        private val differ = AsyncListDiffer(
            RoverRecyclerChangeCallback(this),
            AsyncDifferConfig.Builder(DataDiffUtil).build()
        )

        internal inner class RoverRecyclerChangeCallback(
            private val adapter: FiltersRecyclerAdapter
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
            private val DataDiffUtil = object : DiffUtil.ItemCallback<TextBitmapFilter>() {
                override fun areItemsTheSame(
                    oldItem: TextBitmapFilter,
                    newItem: TextBitmapFilter
                ) =
                    oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: TextBitmapFilter,
                    newItem: TextBitmapFilter
                ) =
                    oldItem == newItem

            }
        }

        override fun getItemCount(): Int {
            return differ.currentList.size
        }
    }