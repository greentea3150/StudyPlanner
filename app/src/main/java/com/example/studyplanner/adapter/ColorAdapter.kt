import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.studyplanner.R

class ColorAdapter(
    private val colors: List<Int>,
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colors[position]
        holder.bind(color)
    }

    override fun getItemCount(): Int = colors.size

    inner class ColorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val colorView: ImageView = view.findViewById(R.id.imageViewColor)

        fun bind(color: Int) {
            colorView.setBackgroundColor(color)
            colorView.setOnClickListener { onColorSelected(color) }
        }
    }
}
