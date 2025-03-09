import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R

class CourseAdapter(
    private var courses: List<Pair<String, String>>,
    private val onLongClick: (Int) -> Unit,
    private val onItemClick: (Pair<String, String>) -> Unit // ✅ 新增点击事件回调
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val courseName: TextView = itemView.findViewById(R.id.textCourseName)
        val major: TextView = itemView.findViewById(R.id.textMajor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.course_item, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val (major, course) = courses[position]
        holder.courseName.text = course
        holder.major.text = major

        holder.itemView.setOnLongClickListener {
            onLongClick(position)
            true
        }

        // ✅ 添加点击事件
        holder.itemView.setOnClickListener {
            onItemClick(courses[position])
        }
    }

    override fun getItemCount(): Int = courses.size

    fun updateData(newCourses: List<Pair<String, String>>) {
        courses = newCourses
        notifyDataSetChanged()
    }
}
