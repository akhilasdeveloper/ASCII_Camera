package com.akhilasdeveloper.asciicamera.ui.recyclerview

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.*
import com.akhilasdeveloper.asciicamera.R
import com.akhilasdeveloper.asciicamera.databinding.CustomFilterListItemBinding
import com.akhilasdeveloper.asciicamera.util.Constants
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters.Companion.FilterSpecs
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DownloadsFiltersRecyclerAdapter(
    private val interaction: RecyclerCustomFiltersClickListener? = null,
    private val sampleBitmap: Bitmap,
    private val scope: CoroutineScope,
    private val resources: Resources
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var selectedID: Long? = -1
        set(value) {
            field = value
            notifyItemChanged(lastSelectedPosition)
            notifyItemChanged(selectedPosition)
            if (value == -1L){
                lastSelectedPosition = -1
                selectedPosition = -1
            }
        }
    private var selectedPosition = -1
    private var lastSelectedPosition = -1
    private val selectionColor =
        ResourcesCompat.getColor(resources, R.color.capture_btn_fill_color, null)
    private val selectionFgColor =
        ResourcesCompat.getColor(resources, R.color.selection_fg_color, null)
    private val unSelectedColor = ResourcesCompat.getColor(resources, R.color.menu_bg_color, null)
    private val unSelectedFgColor = ResourcesCompat.getColor(resources, R.color.menu_fg_color, null)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val bindingPhoto =
            CustomFilterListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(bindingPhoto, interaction, sampleBitmap, scope)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = differ.currentList[position]

        val photoItemViewHolder = holder as PhotoViewHolder

        currentItem?.let {

            photoItemViewHolder.bindPhoto(currentItem, position)

            if (selectedID == it.id) {
                selectedPosition = holder.adapterPosition
                holder.binding.root.setCardBackgroundColor(selectionColor)
                holder.binding.filterName.setTextColor(selectionFgColor)
            } else {
                holder.binding.root.setCardBackgroundColor(Color.TRANSPARENT)
                holder.binding.root.setCardBackgroundColor(unSelectedColor)
                holder.binding.filterName.setTextColor(unSelectedFgColor)
            }

            holder.binding.root.setOnClickListener { v ->
                interaction?.onCustomItemClicked(it, Constants.VIEW_TYPE_DOWNLOADED)
                lastSelectedPosition = selectedPosition
                selectedPosition = holder.adapterPosition
                selectedID = it.id
            }
        }
    }

    class PhotoViewHolder(
        val binding: CustomFilterListItemBinding,
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
                binding.filterItemImage.setImageBitmap(
                    asciiGenerator.imageBitmapToTextBitmap(
                        sampleBitmap
                    )
                )
            }

            binding.filterName.text = photo.name

            binding.tools.visibility = View.GONE
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
        private val adapter: DownloadsFiltersRecyclerAdapter
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